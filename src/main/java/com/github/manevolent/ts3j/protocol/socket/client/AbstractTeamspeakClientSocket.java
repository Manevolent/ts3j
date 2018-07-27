package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody;
import com.github.manevolent.ts3j.protocol.packet.PacketBody6Ack;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyFragment;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.local.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.packet.transformation.DefaultPacketTransformation;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.AbstractTeamspeakSocket;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;

public abstract class AbstractTeamspeakClientSocket
        extends AbstractTeamspeakSocket
        implements TeamspeakClientSocket {
    private static final byte[] INIT1_MAC  = new byte[] {
            0x54, 0x53, 0x33, 0x49, 0x4E, 0x49, 0x54, 0x31
    };

    private final Object connectionStateLock = new Object();
    private final LinkedBlockingDeque<Packet> readQueue = new LinkedBlockingDeque<>(65536);
    private final Map<PacketBodyType, Reassembly> reassemblyQueue = new LinkedHashMap<>();
    private final Map<String, Object> clientOptions = new LinkedHashMap<>();
    private final Map<Integer, PacketResponse> sendQueue = new LinkedHashMap<>();

    private Identity identity = null;
    private ClientConnectionState connectionState = null;
    private PacketTransformation transformation = new DefaultPacketTransformation();
    private PacketHandler handler;

    private int generationId = 0;
    private int clientId = 0;
    private int packetId = 0;

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

    protected abstract NetworkPacket readNetworkPacket() throws IOException;

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

    /**
     * Sends a command to the remote server.
     * @param command Command to send.
     * @return Command response object to track command response.
     * @throws IOException
     */
    public CommandResponse sendCommand(Command command) throws IOException {
        if (command.willExpectResponse() && !isConnected())
            throw new IOException("not connected");

        CommandResponse response;

        if (command.willExpectResponse()) {
            //command.appendParameter(new CommandSingleParameter("return_code", returnCodeIndex));
            response = new CommandResponse(command, 0); // TODO
        } else {
            response = new CommandResponse(command, -1);
        }

        String commandString = command.toString();

        response.setDispatchedTime(System.currentTimeMillis());

        return response;
    }

    @Override
    public void writePacket(PacketBody body) throws IOException, TimeoutException {
        PacketHeader header;

        try {
            header = body.getRole().getHeaderClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        // Construct a network packet
        Packet packet = new Packet(body.getRole());
        packet.setHeader(header);
        packet.setBody(body);

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

        // Ensure type matches
        packet.getHeader().setType(packet.getBody().getType());

        // Set header values
        packet.getBody().setHeaderValues(packet.getHeader());

        if (packet.getHeader().getType() != PacketBodyType.INIT1) {
            packetId++;
            packetId = packetId & 0x0000FFFF;

            if (packetId == 0) generationId++;

            if (packet.getHeader() instanceof ClientPacketHeader)
                ((ClientPacketHeader) packet.getHeader()).setClientId(clientId);

            packet.getHeader().setGeneration(generationId);
            packet.getHeader().setPacketId(packetId);
        }

        if (packet.getHeader().getType() == PacketBodyType.COMMAND ||
                packet.getHeader().getType() == PacketBodyType.COMMAND_LOW)
            packet.getHeader().setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);

        if (packet.getSize() > 500) {
            if (!packet.getHeader().getType().isSplittable())
                throw new IllegalArgumentException("packet too large: " + packet.getSize());

            // Split
            int totalSize = packet.getBody().getSize();

            ByteBuffer outputBuffer = ByteBuffer.allocate(totalSize);
            packet.getBody().write(outputBuffer);

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
            if (!packet.getHeader().getType().canEncrypt())
                throw new IllegalArgumentException("packet flagged as encrypted but this would be a violation");

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
        PacketResponse response;

        if (willAcknowledge)
            sendQueue.put(
                    packet.getHeader().getPacketId(),
                    response = new PacketResponse(
                            networkPacket,
                            Integer.MAX_VALUE
                    )
            );
        else
            response = null;

        Ts3Debugging.debug("[PROTOCOL] WRITE " + networkPacket.getHeader().getType().name());
        for (HeaderFlag flag : HeaderFlag.values())
            Ts3Debugging.debug(flag.name() + ": " + networkPacket.getHeader().getPacketFlag(flag));

        writeNetworkPacket(networkPacket);

        // If necessary, fulfill promise for the packet acknowledgement or throw TimeoutException
        if (response != null) {
            while (response.getRetries() < response.getMaxTries()) {
                try {
                    if (getState() == ClientConnectionState.DISCONNECTED)
                        throw new IllegalStateException("no longer connected");

                    response.getFuture().get(1000L, TimeUnit.MILLISECONDS);
                    break;
                } catch (TimeoutException e) {
                    response.resend();
                } catch (CancellationException e) {
                    throw e;
                } catch (InterruptedException e) {

                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            if (response.getRetries() >= response.getMaxTries())
                throw new TimeoutException("send");
        }
    }

    private Packet readPacketIntl() throws IOException {
        NetworkPacket networkPacket = readNetworkPacket();

        if (networkPacket.getHeader() == null) throw new NullPointerException("header");

        Packet packet = new Packet(networkPacket.getHeader().getRole());
        packet.setHeader(networkPacket.getHeader());

        boolean fragment = networkPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED);

        if (networkPacket.getHeader().getType().isSplittable()) {
            fragment = fragment || reassemblyQueue.get(networkPacket.getHeader().getType()).isReassembling();
        } else if (fragment)
            throw new IllegalArgumentException("packet is fragment, but not splittable");

        boolean encrypted = !networkPacket.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED) &&
                networkPacket.getHeader().getType().canEncrypt();

        if (networkPacket.getHeader().getType().mustEncrypt() && !encrypted)
            throw new IllegalArgumentException("packet is unencrypted, but must encrypt");

        ByteBuffer packetBuffer =
                (ByteBuffer) networkPacket.getBuffer().position(networkPacket.getHeader().getSize());

        // Decrypt
        if (encrypted) {
            packetBuffer = ByteBuffer.wrap(
                    getTransformation().decrypt(
                            networkPacket.getHeader(),
                            packetBuffer,
                            networkPacket.getDatagram().getLength() - networkPacket.getHeader().getSize()
                    )
            );
        }

        Ts3Debugging.debug("[NETWORK] READ " +
                packet.getHeader().getType() + " BODY "
                + Ts3Debugging.getHex(packetBuffer.array())
        );

        // Fragment handling
        if (fragment) {
            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name() + " fragment");

            packet.setBody(
                    new PacketBodyFragment(
                            networkPacket.getHeader().getType(),
                            getRole().getIn()
                    )
            );
        } else {
            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name());
        }

        // Compression?
        // TODO

        // Read packet body
        packet.readBody(packetBuffer);

        return packet;
    }

    @Override
    public Packet readPacket() throws IOException, TimeoutException {
        while (isReading()) {
            Packet packet = readPacketIntl();

            // Find if the packet must be acknowledged
            PacketBodyType ackType = packet.getHeader().getType().getAcknolwedgedBy();
            if (ackType != null) {
                switch (ackType) {
                    case ACK:
                        writePacket(new PacketBody6Ack(getRole().getOut(), packet.getHeader().getPacketId()));
                        break;
                }
            }

            // Find if the packet must be reassembled
            if (packet.getBody() instanceof PacketBodyFragment) {
                packet = reassemblyQueue.get(packet.getHeader().getType()).put(packet);
                if (packet == null) continue;
            }

            // Find if the packet is itself an acknowledgement
            switch (packet.getHeader().getType()) {
                case ACK:
                    int packetId = packet.getHeader().getPacketId();
                    PacketResponse response = sendQueue.get(packetId);
                    if (response != null) response.getFuture().complete(packet);
                    break;
                case ACK_LOW:
                    break;
                default:
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

    private class NetworkHandler implements Runnable {
        @Override
        public void run() {
            while (isReading()) {
                try {
                    Packet packet = readQueue.take();

                    Ts3Debugging.debug("Processing " + packet + "...");
                    getHandler().handlePacket(packet);
                    Ts3Debugging.debug("Processed " + packet + ".");
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
        private final CompletableFuture future = new CompletableFuture<>();
        private long lastSent = 0L;
        private int tries, maxTries;
        private boolean willResend;

        public PacketResponse(NetworkPacket sentPacket,int maxTries) {
            this.sentPacket = sentPacket;
            this.maxTries = maxTries;
            this.lastSent = System.currentTimeMillis();
            this.willResend = maxTries > 0;
        }

        public NetworkPacket getPacket() {
            return sentPacket;
        }

        public void resend() throws IOException {
            AbstractTeamspeakClientSocket.this.writeNetworkPacket(sentPacket);

            tries ++;
        }

        public boolean isWillResend() {
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

        public CompletableFuture getFuture() {
            return future;
        }

        public void setLastSent(long lastSent) {
            this.lastSent = lastSent;
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

            ByteBuffer reassemblyBuffer = ByteBuffer.allocate(totalLength);

            for (Packet old : reassemblyList)
                old.writeBody(reassemblyBuffer);

            Packet firstPacket = reassemblyList.get(0);
            Packet reassembledPacket = new Packet(firstPacket.getRole());
            reassembledPacket.setHeader(firstPacket.getHeader());
            reassembledPacket.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, false);
            reassembledPacket.readBody((ByteBuffer) reassemblyBuffer.position(0));

            return reassembledPacket;
        }

        public Packet put(Packet packet) {
            if (packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
                boolean oldState = state;

                state = !state;

                if (!(packet.getBody() instanceof PacketBodyFragment))
                    throw new IllegalArgumentException("packet fragment object is not representative of a fragment");

                queue.put(packet.getHeader().getPacketId(), packet);

                if (oldState)
                    return reassemble(packet);

                return null;
            } else {
                if (state) {
                    queue.put(packet.getHeader().getPacketId(), packet);

                    return null;
                } else {
                    return packet;
                }
            }
        }

        public boolean isReassembling() {
            return state;
        }
    }
}
