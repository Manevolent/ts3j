package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.*;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.packet.transformation.InitPacketTransformation;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.AbstractTeamspeakSocket;
import com.github.manevolent.ts3j.util.QuickLZ;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import javafx.util.Pair;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server/client common abstract client socket, used to interact with the UDP connection.  Provides most of the protocol
 * such as fragmentation, compression, encryption, and sliding windows.  Handles packet counters and automatically
 * handles ACK and PING types for the higher layer.
 */
public abstract class AbstractTeamspeakClientSocket
        extends AbstractTeamspeakSocket
        implements TeamspeakClientSocket {
    private static final byte[] INIT1_MAC  = new byte[] {
            0x54, 0x53, 0x33, 0x49, 0x4E, 0x49, 0x54, 0x31
    };

    private final Object connectionStateLock = new Object();
    private final LinkedBlockingDeque<Packet> readQueue = new LinkedBlockingDeque<>(65536);
    private final Map<PacketBodyType, Reassembly> reassemblyQueue = new ConcurrentHashMap<>();
    private final Map<String, Object> clientOptions = new ConcurrentHashMap<>();
    private final Map<Integer, PacketResponse> sendQueue = new ConcurrentHashMap<>();
    private final Map<Integer, PacketResponse> sendQueueLow = new ConcurrentHashMap<>();

    private final Map<PacketBodyType, LocalCounter> localSendCounter = new ConcurrentHashMap<>();
    private final Map<PacketBodyType, RemoteCounter> remoteSendCounter = new ConcurrentHashMap<>();

    private CommandProcessor commandProcessor;

    private Identity identity = null;
    private ClientConnectionState connectionState = null;
    private PacketTransformation transformation = new InitPacketTransformation();
    private PacketHandler handler;

    private int generationId = 0;
    private int clientId = 0;
    private int packetId = 0;

    private long lastPing;

    private final NetworkReader networkReader = new NetworkReader();
    private final NetworkHandler networkHandler = new NetworkHandler();
    private Thread networkThread = null;
    private Thread handlerThread = null;

    private boolean reading = false;

    protected AbstractTeamspeakClientSocket() {
        super(SocketRole.CLIENT);

        try {
            setState(ClientConnectionState.DISCONNECTED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Initialize reassembly queue
        for (PacketBodyType bodyType : PacketBodyType.values())
            if (bodyType.isSplittable())
                reassemblyQueue.put(bodyType, new Reassembly());

        // Initialize counters
        for (PacketBodyType bodyType : PacketBodyType.values()) {
            if (bodyType == PacketBodyType.INIT1) {
                localSendCounter.put(bodyType, new LocalCounterZero());
                remoteSendCounter.put(bodyType, new RemoteCounterZero());
            } else {
                localSendCounter.put(bodyType, new LocalCounterFull(65536, true));
                remoteSendCounter.put(bodyType, new RemoteCounterFull(65536, 100));
            }
        }
    }

    protected void start() {
        Thread networkThread = new Thread(networkReader);
        networkThread.setDaemon(true);
        networkThread.start();

        Thread handlerThread = new Thread(networkHandler);
        handlerThread.setDaemon(true);
        handlerThread.start();
    }

    @Override
    public Map<String, Object> getOptions() {
        return clientOptions;
    }

    public final void setSecureParameters(Ts3Crypt.SecureChannelParameters parameters) {
        setTransformation(new PacketTransformation(parameters.getIvStruct(), parameters.getFakeSignature()));
    }

    protected abstract NetworkPacket readNetworkPacket(int timeout) throws IOException;

    protected abstract void writeNetworkPacket(NetworkPacket packet) throws IOException;

    public void setState(ClientConnectionState state) throws IOException, TimeoutException {
        synchronized (connectionStateLock) {
            if (state != this.connectionState) {
                Ts3Debugging.debug("State changing: " + state.name());

                boolean wasDisconnected = this.connectionState == ClientConnectionState.DISCONNECTED;

                if (wasDisconnected) setReading(true);

                this.connectionState = state;

                if (state == ClientConnectionState.DISCONNECTED) setReading(false);

                connectionStateLock.notifyAll();

                if (state == ClientConnectionState.DISCONNECTED)
                    clientOptions.clear();

                Ts3Debugging.debug("State changed: " + state.name());
            }
        }

        PacketHandler handler = getHandler();

        if (handler != null)
            ((LocalClientHandler) handler).handleConnectionStateChanging(state);

        if (handler == null || handler.getClass() != state.getHandlerClass()) {
            Ts3Debugging.debug("Assigning " + state.getHandlerClass().getName() + " handler...");

            setHandler(state.createHandler(this));

            Ts3Debugging.debug("Assigned " + state.getHandlerClass().getName() + " handler.");
        }
    }

    public ClientConnectionState getState() {
        return connectionState;
    }

    public void waitForState(ClientConnectionState state, long wait)
            throws InterruptedException, TimeoutException {
        synchronized (connectionStateLock) {
            long start = System.currentTimeMillis();

            while (connectionState != state && System.currentTimeMillis() - start < wait) {
                connectionStateLock.wait(Math.max(0L, wait - (System.currentTimeMillis() - start)));
            }

            if (connectionState != state)
                throw new TimeoutException("timeout waiting for " + state.name() + " state");
        }
    }

    public boolean isConnected() {
        return connectionState == ClientConnectionState.CONNECTED;
    }

    private PacketHeader newOutgoingHeader() {
        PacketHeader header;

        try {
            header = getRole().getOut().getHeaderClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return header;
    }

    @Override
    public void writePacket(PacketBody body) throws IOException, TimeoutException {
        // Construct a network packet
        Packet packet = new Packet(body.getRole());
        packet.setBody(body);

        // Generate header
        PacketHeader header = newOutgoingHeader();

        // Ensure type matches
        header.setType(packet.getBody().getType());

        // Set header values
        body.setHeaderValues(header);

        if (header instanceof ClientPacketHeader)
            ((ClientPacketHeader) header).setClientId(clientId);

        // Count packet
        Pair<Integer, Integer> counter;

        switch (header.getType()) {
            case ACK:
                counter = new Pair<>(
                        ((PacketBody6Ack)packet.getBody()).getPacketId(),
                        remoteSendCounter.get(PacketBodyType.ACK).getGeneration(
                                ((PacketBody6Ack) packet.getBody()).getPacketId()
                        )
                );
                break;
            case ACK_LOW:
                counter = new Pair<>(
                        ((PacketBody7AckLow)packet.getBody()).getPacketId(),
                        remoteSendCounter.get(PacketBodyType.ACK_LOW).getGeneration(
                                ((PacketBody7AckLow) packet.getBody()).getPacketId()
                        )
                );
                break;
            case PONG:
                counter = new Pair<>(
                        ((PacketBody5Pong)packet.getBody()).getPacketId(),
                        remoteSendCounter.get(PacketBodyType.PONG).getGeneration(
                                ((PacketBody5Pong) packet.getBody()).getPacketId()
                        )
                );
                break;
            /*case PING:
                counter = localSendCounter.get(PacketBodyType.PING).current();
                break;*/
            case INIT1:
                counter = new Pair<>(101, 0);
                break;
            default:
                counter = localSendCounter.get(header.getType()).next();
                break;
        }

        header.setPacketId(counter.getKey());
        header.setGeneration(counter.getValue());

        switch (header.getType()) {
            case INIT1:
            case PONG:
            case PING:
                // setHeaderValues will do this, too
                header.setPacketFlag(HeaderFlag.UNENCRYPTED, true);
                break;
            case COMMAND:
            case COMMAND_LOW:
                header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
                break;
            case VOICE:
                // > X is a ushort in H2N order of an own audio packet counter
                //     it seems it can be the same as the packet counter so we will let the packethandler do it.
                ((PacketBody0Voice)body).setPacketId(header.getPacketId());
                break;
        }

        packet.setHeader(header);

        writePacket(packet);
    }

    @Override
    public void writePacket(Packet packet) throws IOException, TimeoutException {
        if (packet.getHeader() == null)
            throw new NullPointerException("header");
        else if (packet.getHeader().getRole() != getRole().getOut())
            throw new IllegalArgumentException("packet role mismatch: " +
                    packet.getHeader().getRole().name() + " != " +
                    getRole().getOut().name());

        if (packet.getSize() > 500) {
            int totalSize = packet.getBody().getSize();

            ByteBuffer outputBuffer = ByteBuffer.allocate(totalSize);
            packet.getBody().write(outputBuffer);

            if (packet.getHeader().getType().isCompressible()) {
                byte[] compressed = QuickLZ.compress(outputBuffer.array(), 1);
                packet.getHeader().setPacketFlag(HeaderFlag.COMPRESSED, true);
                packet.setBody(new PacketBodyCompressed(packet.getHeader().getType(), packet.getRole(), compressed));

                if (packet.getSize() <= 500) {
                    writePacketIntl(packet);
                    return;
                }
            }

            if (!packet.getHeader().getType().isSplittable())
                throw new IllegalArgumentException("packet too large: " + packet.getSize() + " (cannot split)");

            for (int offs = 0; offs < totalSize;) {
                int flush = Math.min(500 - packet.getHeader().getSize(), totalSize - offs);
                boolean first = offs == 0;
                boolean last = flush < 500;

                Packet piece = new Packet(packet.getRole());
                piece.setHeader(packet.getHeader());

                if (!first) // Only first packet has flags
                    piece.getHeader().setPacketFlags(HeaderFlag.NONE.getIndex());

                // First and last packet get FRAGMENTED flag
                piece.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, first || last);

                byte[] pieceBytes = new byte[flush];
                System.arraycopy(outputBuffer.array(), offs, pieceBytes, 0, flush);

                piece.setBody(new PacketBodyFragment(
                        packet.getHeader().getType(),
                        packet.getHeader().getRole(),
                        pieceBytes
                ));

                writePacketIntl(piece);

                offs += flush;
            }
        } else {
            writePacketIntl(packet);
        }
    }

    private void writePacketIntl(Packet packet) throws IOException, TimeoutException {
        // Flush to a buffer
        ByteBuffer outputBuffer;

        if (!packet.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED)) {
            Ts3Debugging.debug("[PROTOCOL] ENCRYPT " +
                    packet.getHeader().getType().name() + " generation=" +
                    packet.getHeader().getGeneration());

            outputBuffer = getTransformation().encrypt(packet);
        } else {
            if (packet.getHeader().getType() == PacketBodyType.INIT1) {
                System.arraycopy(INIT1_MAC, 0, packet.getHeader().getMac(), 0, 8);
            } else {
                System.arraycopy(transformation.getFakeMac(), 0, packet.getHeader().getMac(), 0, 8);
            }

            outputBuffer = ByteBuffer.allocate(packet.getSize());

            outputBuffer.order(ByteOrder.BIG_ENDIAN);
            packet.writeHeader(outputBuffer);
            packet.writeBody(outputBuffer);
        }

        NetworkPacket networkPacket = new NetworkPacket(
                new DatagramPacket(outputBuffer.array(), outputBuffer.position()),
                packet.getHeader(),
                (ByteBuffer) outputBuffer.position(0)
        );

        // Find if the packet must be acknowledged and create a promise for that action
        boolean willAcknowledge = packet.getHeader().getType().getAcknolwedgedBy() != null;
        final PacketResponse response;

        if (willAcknowledge)
            sendQueue.put(
                    packet.getHeader().getPacketId(),
                    response = new PacketResponse(
                            networkPacket,
                            sendQueue,
                            networkPacket.getHeader().getType().canResend() ? 10 : 0
                    )
            );
        else
            response = null;

        Ts3Debugging.debug("[PROTOCOL] WRITE " + networkPacket.getHeader().getType().name());

        writeNetworkPacket(networkPacket);

        // If necessary, fulfill promise for the packet acknowledgement or throw TimeoutException
        if (response != null && networkPacket.getHeader().getType() != PacketBodyType.PING) {
            while (response.getRetries() < response.getMaxTries()) {
                try {
                    if (getState() == ClientConnectionState.DISCONNECTED)
                        throw new IllegalStateException("no longer connected");

                    // Wait one cycle for an ACK
                    response.waitForAcknowledgement(1000);
                    break;
                } catch (TimeoutException e) {
                    if (response.willResend())
                        response.resend();
                    else
                        throw e;
                } catch (CancellationException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (response.getRetries() >= response.getMaxTries())
                throw new TimeoutException("send");
        }
    }

    private Packet readPacketIntl(NetworkPacket networkPacket) throws IOException {
        if (networkPacket.getHeader() == null) throw new NullPointerException("header");

        Packet packet = new Packet(networkPacket.getHeader().getRole());
        packet.setHeader(networkPacket.getHeader());

        boolean fragment = networkPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED);

        if (networkPacket.getHeader().getType().isSplittable()) {
            fragment = fragment || reassemblyQueue.get(networkPacket.getHeader().getType()).isReassembling();
        } else if (fragment)
            throw new IllegalArgumentException(
                    networkPacket.getHeader().getType() +
                    "packet is fragment, but not splittable"
            );

        boolean encrypted = !networkPacket.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED);

        if (networkPacket.getHeader().getType().mustEncrypt() && !encrypted)
            throw new IllegalArgumentException(
                    networkPacket.getHeader().getType() +
                    " is unencrypted, but must be encrypted"
            );

        ByteBuffer packetBuffer = networkPacket.getBuffer();

        // Decrypt
        if (encrypted) {
            Ts3Debugging.debug("[PROTOCOL] DECRYPT " + networkPacket.getHeader().getType().name()
                    + " generation=" + networkPacket.getHeader().getGeneration());

            try {
                packetBuffer = ByteBuffer.wrap(
                        getTransformation().decrypt(
                                networkPacket.getHeader(),
                                packetBuffer,
                                networkPacket.getDatagram().getLength() -
                                        networkPacket.getHeader().getSize()
                        )
                ).order(ByteOrder.BIG_ENDIAN);
            } catch (InvalidCipherTextException e) {
                throw new IOException("failed to decrypt " + networkPacket.getHeader().getType().name(), e);
            }
        }

        // Fragment handling
        if (fragment) {
            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name() + " fragment");

            packet.setBody(new PacketBodyFragment(networkPacket.getHeader().getType(), getRole().getIn()));
        } else {
            if (packet.getHeader().getPacketFlag(HeaderFlag.COMPRESSED))
                packet.setBody(new PacketBodyCompressed(networkPacket.getHeader().getType(), getRole().getIn()));

            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name());
        }

        // Read raw packet body
        packet.readBody(packetBuffer);

        // Finally, handle compressed bodies
        if (packet.getBody() instanceof PacketBodyCompressed) {
            Ts3Debugging.debug("[PROTOCOL] DECOMPRESS " + networkPacket.getHeader().getType().name());

            byte[] decompressed = QuickLZ.decompress(((PacketBodyCompressed) packet.getBody()).getCompressed());

            packetBuffer = ByteBuffer
                    .wrap(decompressed)
                    .order(ByteOrder.BIG_ENDIAN);

            packet = new Packet(packet.getRole(), packet.getHeader());

            packet.readBody(packetBuffer);
        }

        // Return to parent handler
        return packet;
    }

    @Override
    public Packet readPacket() throws
            IOException,
            TimeoutException {
        NetworkPacket networkPacket;
        long timeout;

        while (isReading()) {
            try {
                timeout = getState() == ClientConnectionState.CONNECTED ?
                        Math.min(1000L, 1000L - (System.currentTimeMillis() - lastPing)) :
                        Integer.MAX_VALUE;

                if (timeout <= 0) throw new SocketTimeoutException();

                networkPacket = readNetworkPacket((int) timeout);
            } catch (SocketTimeoutException e) {
                if (getState() == ClientConnectionState.CONNECTED) {
                    writePacket(new PacketBody4Ping(getRole().getOut()));
                    lastPing = System.currentTimeMillis();
                }

                continue;
            }

            // Get packet generation
            RemoteCounter counter = remoteSendCounter.get(networkPacket.getHeader().getType());
            int generation = 0;
            if (counter != null) {
                generation = counter.getGeneration(networkPacket.getHeader().getPacketId());
            }
            networkPacket.getHeader().setGeneration(generation);

            // Read packet (decrypt, decompress, etc)
            Packet packet = readPacketIntl(networkPacket);

            // Find if the packet must be acknowledged
            PacketBodyType ackType = packet
                    .getHeader()
                    .getType()
                    .getAcknolwedgedBy();

            if (ackType != null) {
                switch (ackType) {
                    case ACK:
                        Ts3Debugging.debug("[PROTOCOL] Acknowledge " + packet + " " + packet.getHeader().getType().name()
                                + " id=" + packet.getHeader().getPacketId());
                        writePacket(new PacketBody6Ack(getRole().getOut(), packet.getHeader().getPacketId()));
                        break;
                    case ACK_LOW:
                        writePacket(new PacketBody7AckLow(getRole().getOut(), packet.getHeader().getPacketId()));
                        break;
                    case PONG:
                        Packet pong = new Packet(
                                getRole().getOut(),
                                newOutgoingHeader()
                        );

                        if (pong.getHeader() instanceof ClientPacketHeader)
                            ((ClientPacketHeader) pong.getHeader()).setClientId(getClientId());

                        pong.getHeader().setPacketFlag(HeaderFlag.UNENCRYPTED, true);
                        pong.getHeader().setPacketId(packet.getHeader().getPacketId());
                        pong.setBody(new PacketBody5Pong(getRole().getOut(), packet.getHeader().getPacketId()));

                        writePacket(pong);

                        continue; // Don't pass pings to the parent
                }
            }

            // Find if the packet must be reassembled
            if (packet.getHeader().getType().isSplittable()) {
                packet = reassemblyQueue.get(packet.getHeader().getType()).put(packet);
                if (packet == null) continue;
            }

            // Find if the packet is itself an acknowledgement
            boolean handle;
            final PacketResponse response;
            switch (packet.getHeader().getType()) {
                case ACK:
                    response = sendQueue.get(((PacketBody6Ack)packet.getBody()).getPacketId());
                    handle = false;
                    break;
                case ACK_LOW:
                    response = sendQueueLow.get(((PacketBody7AckLow)packet.getBody()).getPacketId());
                    handle = false;
                    break;
                case PONG:
                    response = null;
                    handle = false;
                    break;
                default:
                    handle = true;
                    response = null;
            }

            if (packet.getHeader().getType().canResend() && packet.getHeader().getType() != PacketBodyType.INIT1) {
                // Find if we already acknowledged this packet
                handle = handle & counter.put(packet.getHeader().getPacketId());
            }

            if (response != null) {
                response.acknowledge(packet);
            } else if (handle) {
                return packet;
            }
        }

        throw new IllegalStateException("no longer reading");
    }

    @Override
    public PacketTransformation getTransformation() {
        return transformation;
    }

    @Override
    public void setTransformation(PacketTransformation transformation) {
        this.transformation = transformation;
    }

    @Override
    public PacketHandler getHandler() {
        return handler;
    }

    protected void setHandler(PacketHandler handler) throws IOException, TimeoutException {
        if (this.handler != handler) {
            if (this.handler != null) this.handler.onUnassigning();
            this.handler = handler;
            if (handler != null) handler.onAssigned();
        }
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    protected boolean isReading() {
        return reading;
    }

    protected void setReading(boolean b) {
        if (this.reading != b) {
            this.reading = b;

            if (!reading) {
                if (networkThread != null) networkThread.interrupt();
                if (handlerThread != null) handlerThread.interrupt();
            } else {
                start();
            }
        }
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public int getGenerationId() {
        return generationId;
    }

    public void setGenerationId(int generationId) {
        this.generationId = generationId;
    }

    public String getNickname( ){
        return getOption("client.nickname", String.class);
    }

    public void setNickname(String name) {
        setOption("client.nickname", name);
    }

    public CommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }

    private class NetworkHandler implements Runnable {
        @Override
        public void run() {
            while (isReading()) {
                try {
                    Packet packet = readQueue.take();

                    // separate these so we getHandler at proper runtime instant rather than get it and let it change
                    // on us

                    getHandler().handlePacket(packet);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) continue;

                    Ts3Debugging.debug("Problem handling packet", e);
                }
            }
        }
    }

    private class NetworkReader implements Runnable {
        @Override
        public void run() {
            while (isReading()) {
                try {
                    readQueue.put(readPacket());
                } catch (Exception e) {
                    if (e instanceof InterruptedException) continue;

                    Ts3Debugging.debug("Problem reading packet", e);
                }
            }
        }
    }

    private class PacketResponse {
        private final NetworkPacket sentPacket;
        private final CompletableFuture<Packet> future = new CompletableFuture<>();
        private final Map<Integer, PacketResponse> responsibleQueue;
        private long lastSent = 0L;
        private int tries = 1, maxTries;
        private boolean willResend;

        public PacketResponse(NetworkPacket sentPacket, Map<Integer, PacketResponse> responsibleQueue, int maxTries) {
            this.sentPacket = sentPacket;
            this.responsibleQueue = responsibleQueue;
            this.maxTries = maxTries;
            this.lastSent = System.currentTimeMillis();
            this.willResend = maxTries > 0;
        }

        public NetworkPacket getPacket() {
            return sentPacket;
        }

        public void resend() throws IOException {
            AbstractTeamspeakClientSocket.this.writeNetworkPacket(sentPacket);
            setLastSent(System.currentTimeMillis());
            tries ++;
        }

        /**
         * Waits for a package acknowledgement.
         * @return true if the acknowledgement wasn't null (shouldn't ever happen)
         * @throws ExecutionException
         * @throws InterruptedException
         */
        public boolean waitForAcknowledgement()
                throws ExecutionException, InterruptedException {
            Packet acknowledgement = future.get();
            return acknowledgement != null;
        }

        /**
         * Waits for a package acknowledgement.
         * @param millis Milliseconds to wait.  When transpired with no completion, TimeoutException is thrown.
         * @return true if the acknowledgement wasn't null (shouldn't ever happen)
         * @throws ExecutionException
         * @throws InterruptedException
         */
        public boolean waitForAcknowledgement(long millis)
                throws TimeoutException, ExecutionException, InterruptedException {
            Packet acknowledgement = future.get(millis, TimeUnit.MILLISECONDS);

            return acknowledgement != null;
        }

        public boolean willResend() {
            return willResend;
        }

        public int getMaxTries() {
            return maxTries;
        }

        public int getRetries() {
            return tries;
        }

        public long getLastSent() {
            return lastSent;
        }

        public void acknowledge(Packet p) {
            responsibleQueue.remove(sentPacket.getHeader().getPacketId());
            future.complete(p);
        }

        public void setLastSent(long lastSent) {
            this.lastSent = lastSent;
        }
    }

    public interface RemoteCounter {

        /**
         * Gets the generation for the specified packet ID.
         *
         * @param packetId packet ID
         * @return expected generation
         */
        int getGeneration(int packetId);

        /**
         * Gets the current generation of the counter.
         *
         * @return current generation
         */
        int getCurrentGeneration();

        /**
         * Counts the given packet by its packet ID and stores it in history.
         *
         * @param packetId packet to put
         * @return true if the packet was placed, false otherwise
         */
        boolean put(int packetId);

    }

    public static class RemoteCounterZero implements RemoteCounter {

        @Override
        public int getGeneration(int packetId) {
            return 0;
        }

        @Override
        public int getCurrentGeneration() {
            return 0;
        }

        @Override
        public boolean put(int packetId) {
            return true;
        }

    }

    public static class RemoteCounterFull implements RemoteCounter {
        private final Integer[] buffer; // history buffer
        private final int bufferSize; // size of the packet history buffer, used for remembering received packets
        private final int generationSize; // size of the physical window, typically 65536 for a 16-bit/ushort window field

        private Pair<Integer, Integer>
                bufferStart = new Pair<>(0,0), // position of the start of the buffer in the window
                bufferEnd; // position of the end of the buffer in the window

        /**
         * Constructs a new full counter
         *
         * @param windowSize size of the packet history buffer, used for remembering received packets
         * @param generationSize length of any given generation, typically 65536 for a 16-bit/ushort window field
         */
        public RemoteCounterFull(int generationSize, int windowSize) { // most often will be 65536, 100
            if (windowSize >= generationSize) throw new IllegalArgumentException("windowSize > generationSize");

            this.buffer = new Integer[windowSize];
            this.bufferSize = windowSize;
            this.generationSize = generationSize;

            this.bufferEnd = new Pair<>(0, windowSize - 1);
        }

        @Override
        public int getGeneration(int packetId) {
            packetId %= generationSize;

            // find if right of the start and within the start's bounds
            if (packetId >= bufferStart.getValue()
                    && packetId < bufferStart.getValue() + bufferSize
                    && packetId < generationSize) {
                return bufferStart.getKey();
            }

            // find if left of end and within the end's bounds
            if (packetId <= bufferEnd.getValue()
                    && packetId > 0
                    && packetId > bufferEnd.getValue() - bufferSize
                    && packetId < generationSize) {
                return bufferEnd.getKey();
            }

            // find if outside of buffer to the right
            if (packetId > bufferEnd.getValue())
                return bufferEnd.getKey(); // ahead in current generation

            // find if outside of buffer to the left
            if (packetId < bufferStart.getValue())
                return bufferEnd.getKey() + 1; // modulated wrap, ahead of current generation

            // should never arrive here.
            throw new IllegalStateException();
        }

        @Override
        public int getCurrentGeneration() {
            return bufferEnd.getKey();
        }

        @Override
        public boolean put(int packetId) {
            // find if right of the start and within the start's bounds
            if (packetId >= bufferStart.getValue()
                    && packetId < bufferStart.getValue() + bufferSize
                    && packetId < generationSize) {
                return putRelative(packetId - bufferStart.getValue(), bufferStart.getKey());
            }

            // find if left of end and within the end's bounds
            if (packetId <= bufferEnd.getValue()
                    && packetId >= 0
                    && packetId > bufferEnd.getValue() - bufferSize
                    && packetId < generationSize) {
                return putRelative(bufferSize - (bufferEnd.getValue() - packetId) - 1, bufferEnd.getKey());
            }

            // distance the start position must move right by
            int amountMoved = 0;

            // find if outside of buffer to the right
            if (packetId > bufferEnd.getValue()) { // slide buffer forward uniformly
                amountMoved = packetId - bufferEnd.getValue();
            }

            // find if outside of buffer to the left
            if (packetId < bufferStart.getValue()) { // start wrapping buffer or continue to do so
                amountMoved =
                        (generationSize - bufferStart.getValue()) +
                        packetId + 1;

                amountMoved -= bufferSize;
            }

            if (amountMoved > 0) { // should always be true
                int toMove = Math.max(0, bufferSize - amountMoved);
                Ts3Debugging.debug(Arrays.toString(buffer));
                if (toMove > 0 && toMove < bufferSize)
                    System.arraycopy(buffer, amountMoved, buffer, 0, toMove);

                int toNullify = Math.max(0, Math.min(bufferSize, amountMoved));
                for (int i = bufferSize - toNullify; i < bufferSize; i ++)
                    buffer[i] = null;

                Ts3Debugging.debug(Arrays.toString(buffer));
                int bufferStartGeneration = bufferStart.getKey();
                int bufferStartPosition = bufferStart.getValue();

                bufferStartPosition += amountMoved;
                if (bufferStartPosition >= generationSize) {
                    bufferStartPosition %= generationSize;
                    bufferStartGeneration ++;
                }

                int bufferEndGeneration;
                int bufferEndPosition = (bufferStartPosition + bufferSize - 1) % generationSize;
                if (bufferEndPosition < bufferStartPosition)
                    bufferEndGeneration = bufferStartGeneration + 1;
                else
                    bufferEndGeneration = bufferStartGeneration;

                this.bufferStart = new Pair<>(bufferStartGeneration, bufferStartPosition);
                this.bufferEnd = new Pair<>(bufferEndGeneration, bufferEndPosition);
            } else
                throw new IllegalStateException();

            // if we had to adjust the buffer size, recursively retry put
            return put(packetId);
        }

        private boolean putRelative(int index, int generation) {
            Integer existing = buffer[index];
            if (existing == null || existing != generation) {
                buffer[index] = generation;
                return true;
            } else {
                return false;
            }
        }
    }



    public static class RemoteCounterFullModulated extends RemoteCounterFull {
        private final int modulationSize;
        private final int modulationsPerGeneration;

        /**
         * Constructs a new full, modulated counter
         * This counter is used to sub-modulate a full packet counter, in smaller increments, while still using
         * the same code in the backend to modulate the physical window.
         *
         * @param generationSize length of any given generation, typically 65536 for a 16-bit/ushort window field
         * @param windowSize     size of the packet history buffer, used for remembering received packets
         * @param modulationSize slots within the generation to extrapolated generations by
         */
        public RemoteCounterFullModulated(int generationSize, int windowSize, int modulationSize) {
            super(generationSize, windowSize);

            if (generationSize % modulationSize != 0)
                throw new IllegalArgumentException("modulationSize invalid");

            this.modulationSize = modulationSize;
            this.modulationsPerGeneration = generationSize / modulationSize;
        }

        private int calculateSubGeneration(int packetId) {
            return packetId == 0 ? 0 : (int) Math.floor(packetId / modulationSize);
        }

        @Override
        public int getGeneration(int packetId) {
            return (super.getGeneration(packetId) * modulationsPerGeneration) + calculateSubGeneration(packetId);
        }

        @Override
        public int getCurrentGeneration() {
            // We can't exactly do this right now.  This will require extra work.
            throw new UnsupportedOperationException();
        }
    }

    public interface LocalCounter {
        Pair<Integer, Integer> next();
        int getPacketId();
        boolean setPacketId(int packetId);
        int getGeneration();
        void setGeneration(int i);

        default Pair<Integer,Integer> current() {
            return new Pair<>(getPacketId(), getGeneration());
        }
    }

    public static class LocalCounterZero implements LocalCounter {
        @Override
        public Pair<Integer, Integer> next() {
            return new Pair<>(0,0);
        }

        @Override
        public int getPacketId() {
            return 0;
        }

        @Override
        public boolean setPacketId(int packetId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getGeneration() {
            return 0;
        }

        @Override
        public void setGeneration(int i) {
            throw new UnsupportedOperationException();
        }
    }

    public static class LocalCounterFull implements LocalCounter {
        private final Object sendLock = new Object();
        private final boolean counting;
        private final int generationSize;

        private int packetId = 0;
        private int generationId = 0;

        public LocalCounterFull(int generationSize, boolean counting) {
            this.generationSize = generationSize;
            this.counting = counting;
        }

        public LocalCounterFull(int generationSize, int start, boolean counting) {
            this.generationSize = generationSize;
            this.counting = counting;

            setPacketId(start);
        }

        public int getPacketId() {
            return packetId;
        }

        @Override
        public boolean setPacketId(int packetId) {
            synchronized (sendLock) {
                if (this.packetId == packetId) return false;

                if (packetId >= generationSize) {
                    this.packetId = 0; // wrap around
                    if (isCounting()) setGeneration(getGeneration() + 1);
                } else {
                    this.packetId = packetId;
                }

                return true;
            }
        }

        public int getGeneration() {
            return generationId;
        }

        @Override
        public void setGeneration(int i) {
            synchronized (sendLock) {
                Ts3Debugging.debug("setGeneration: " + i);
                this.generationId = i;
            }
        }

        @Override
        public Pair<Integer, Integer> next() {
            synchronized (sendLock) {
                setPacketId(getPacketId() + 1);
                return current();
            }
        }

        public boolean isCounting() {
            return counting;
        }
    }

    private class Reassembly {
        private final Map<Integer, Packet> queue = new LinkedHashMap<>();
        private boolean state = false;

        public Packet reassemble(Packet lastFragment) {
            // Pull out all other packets in the queue before this one which would also be fragmented
            List<Packet> reassemblyList = new ArrayList<>();
            for (int packetId = lastFragment.getHeader().getPacketId();; packetId--) {
                Packet olderPacket = queue.get(packetId);

                if (olderPacket == null)
                    break; // Chain breaks here
                else if (olderPacket.getHeader().getType() != lastFragment.getHeader().getType())
                    continue; // skip
                else {
                    reassemblyList.add(olderPacket);
                }
            }

            // Reverse the collection
            Collections.reverse(reassemblyList);

            // Rebuild a master packet from the contents of all previous packet fragments
            int totalLength = reassemblyList.stream().mapToInt(x -> x.getBody().getSize()).sum() + lastFragment.getBody().getSize();
            if (totalLength < 0) throw new IllegalArgumentException("reassembly too small: " + totalLength);

            Packet firstPacket = reassemblyList.get(0);
            Packet reassembledPacket = new Packet(firstPacket.getRole());
            reassembledPacket.setHeader(firstPacket.getHeader());

            ByteBuffer reassemblyBuffer = ByteBuffer.allocate(totalLength);

            reassembledPacket.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, false);

            for (Packet old : reassemblyList)
                old.writeBody(reassemblyBuffer);

            if (firstPacket.getHeader().getPacketFlag(HeaderFlag.COMPRESSED))
                reassemblyBuffer = ByteBuffer.wrap(QuickLZ.decompress(reassemblyBuffer.array()));

            reassembledPacket.readBody(reassemblyBuffer);

            return reassembledPacket;
        }

        public Packet put(Packet packet) {
            Packet reassembled;

            if (packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
                boolean oldState = state;

                state = !state;

                if (!(packet.getBody() instanceof PacketBodyFragment))
                    throw new IllegalArgumentException("packet fragment object is not representative of a fragment");

                queue.put(packet.getHeader().getPacketId(), packet);

                if (oldState) {
                    reassembled = reassemble(packet);
                } else {
                    reassembled = null;
                }
            } else {
                if (state) {
                    queue.put(packet.getHeader().getPacketId(), packet);

                    reassembled = null;
                } else {
                    reassembled = packet;
                }
            }

            return reassembled;
        }

        public boolean isReassembling() {
            return state;
        }
    }
}
