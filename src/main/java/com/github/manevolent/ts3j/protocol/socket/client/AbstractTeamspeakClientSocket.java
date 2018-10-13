package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.PacketKind;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.*;
import com.github.manevolent.ts3j.protocol.packet.fragment.Fragments;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketBodyFragment;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketReassembly;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.packet.statistics.PacketStatistics;
import com.github.manevolent.ts3j.protocol.packet.transformation.InitPacketTransformation;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.AbstractTeamspeakSocket;
import com.github.manevolent.ts3j.util.QuickLZ;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import com.github.manevolent.ts3j.util.Pair;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Server/client common abstract client socket, used to interact with the UDP connection.  Provides most of the protocol
 * such as fragmentation, compression, encryption, and sliding windows.  Handles packet counters and automatically
 * handles ACK and PING types for the higher layer.
 */
public abstract class AbstractTeamspeakClientSocket
        extends AbstractTeamspeakSocket
        implements TeamspeakClientSocket {
    public static long TIMEOUT = 30000L;

    private static final byte[] INIT1_MAC  = new byte[] {
            0x54, 0x53, 0x33, 0x49, 0x4E, 0x49, 0x54, 0x31
    };

    private final Map<String, Object> clientOptions = new ConcurrentHashMap<>();

    private final Object connectionStateLock = new Object();

    private final LinkedBlockingDeque<Packet> readQueue = new LinkedBlockingDeque<>(65536);

    private final Map<PacketBodyType, PacketReassembly> reassemblyQueue = new ConcurrentHashMap<>();

    private final Map<Integer, PacketResponse> pingQueue = new ConcurrentHashMap<>();
    private final Map<Integer, PacketResponse> sendQueue = new ConcurrentHashMap<>();
    private final Map<Integer, PacketResponse> sendQueueLow = new ConcurrentHashMap<>();

    private final Map<PacketBodyType, LocalCounter> localSendCounter = new ConcurrentHashMap<>();
    private final Map<PacketBodyType, RemoteCounter> remoteSendCounter = new ConcurrentHashMap<>();

    private final Map<PacketBodyType, Object> sendLocks = new HashMap<>();

    private CommandProcessor commandProcessor;

    private Consumer<Packet> voiceHandler;
    private Consumer<Packet> whisperHandler;

    private Consumer<Throwable> exceptionHandler = Throwable::printStackTrace;

    private Identity identity = null;
    private ClientConnectionState connectionState = null;
    private PacketTransformation transformation = new InitPacketTransformation();
    private PacketHandler handler;

    private int clientId = 0;

    private long lastResponse;
    private long lastPing;

    // RTT
    private static final double alphaSmooth = 0.125D;
    private static final double betaSmooth = 0.25D;
    private double smoothedRtt;
    private double smoothedRttVar;
    private double currentRto;
    //

    // Ping
    private List<Double> pings = new LinkedList<>();
    private Map<PacketKind, PacketStatistics> packetStatistics = new LinkedHashMap<>();
    //

    private final List<Packet> handlerBacklog = new LinkedList<>();

    private final NetworkReader networkReader = new NetworkReader();
    private Thread networkThread = null;

    private final NetworkHandler networkHandler = new NetworkHandler();
    private Thread handlerThread = null;

    private boolean reading = false;

    protected AbstractTeamspeakClientSocket(SocketRole role) {
        super(role);

        // Initialize reassembly queue
        for (PacketBodyType bodyType : PacketBodyType.values())
            if (bodyType.isSplittable())
                reassemblyQueue.put(bodyType, new PacketReassembly());

        // Initialize counters
        for (PacketBodyType bodyType : PacketBodyType.values()) {
            if (bodyType == PacketBodyType.INIT1) {
                localSendCounter.put(bodyType, new LocalCounterZero());
                remoteSendCounter.put(bodyType, new RemoteCounterZero());
            } else {
                localSendCounter.put(bodyType, new LocalCounterFull(65536, true));
                remoteSendCounter.put(bodyType, new RemoteCounterFull(65536, 100));
            }

            sendLocks.put(bodyType, new Object());
        }

        // Initialize statistics
        for (PacketKind kind : PacketKind.values()) {
            packetStatistics.put(kind, new PacketStatistics());
        }

        try {
            setState(ClientConnectionState.DISCONNECTED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PacketStatistics getStatistics(PacketKind kind) {
        return packetStatistics.get(kind);
    }

    protected abstract Class<? extends PacketHandler> getHandlerClass(ClientConnectionState state);

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

                boolean initialState = this.connectionState == null;
                boolean wasDisconnected = this.connectionState == ClientConnectionState.DISCONNECTED;

                if (wasDisconnected) setReading(true);

                this.connectionState = state;

                if (state == ClientConnectionState.DISCONNECTED) setReading(false);

                connectionStateLock.notifyAll();

                if (state == ClientConnectionState.DISCONNECTED && !initialState)
                    onDisconnect();

                Ts3Debugging.debug("State changed: " + state.name());
            }
        }

        PacketHandler handler = getHandler();

        if (handler != null ) {
            handler.handleConnectionStateChanging(state);
        }

        Class<? extends PacketHandler> handlerClass = getHandlerClass(state);

        if (handler == null || handler.getClass() != handlerClass) {
            Ts3Debugging.debug("Assigning " + handlerClass + " handler...");

            try {
                setHandler(createHandler(handlerClass));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            Ts3Debugging.debug("Assigned " + handlerClass + " handler.");
        }
    }

    protected void onDisconnect() {
        clientId = -1;

        readQueue.clear();

        handlerBacklog.clear();

        for (PacketBodyType bodyType : PacketBodyType.values())
            if (bodyType.isSplittable())
                reassemblyQueue.get(bodyType).reset();

        pingQueue.clear();
        sendQueue.clear();
        sendQueueLow.clear();

        for (PacketBodyType bodyType : PacketBodyType.values()) {
            localSendCounter.get(bodyType).reset();
            remoteSendCounter.get(bodyType).reset();
        }

        for (PacketKind kind : PacketKind.values()) {
            packetStatistics.put(kind, new PacketStatistics());
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
        // Construct a packet object
        Packet packet = new Packet(body.getRole());

        // Set body
        packet.setBody(body);

        // Generate new header (values get filled in as we go, and primarily in writePacketIntl)
        PacketHeader header = newOutgoingHeader();

        // Ensure type matches
        header.setType(packet.getBody().getType());

        // Set header values
        body.setHeaderValues(header);

        if (header instanceof ClientPacketHeader)
            ((ClientPacketHeader) header).setClientId(clientId);

        // Static header assignments (protocol expectations based on type)
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
        }

        // Assign header pointer
        packet.setHeader(header);

        // Lower-level write; helper is done
        writePacket(packet);
    }

    @Override
    public void writePacket(Packet packet) throws IOException, TimeoutException {
        // Sanity checks.  People are insane!
        if (packet.getHeader() == null)
            throw new NullPointerException("header");
        else if (packet.getHeader().getType() == null)
            throw new NullPointerException("header type");
        else if (packet.getHeader().getRole() != getRole().getOut())
            throw new IllegalArgumentException("packet role mismatch: " +
                    packet.getHeader().getRole().name() + " != " +
                    getRole().getOut().name());

        if (packet.getHeader().getType().isSplittable()) {
            // Split up the outgoing packet into valid pieces
            // it's better to do this all at once instead of sending them as they are split,
            // because we might get halfway through splitting them then throw an exception and
            // we can't write the rest.
            List<Packet> packets = Fragments.split(packet);

            // Note, it's very important we send these packets synchronously.
            // we allow the split to happen multi-thread, but we block until all
            // chunks are sent synchronously.  So, all chunks of packets are synchronous.
            // this happens because we care that the packet IDs are contiguous.
            synchronized (sendLocks.get(packet.getHeader().getType())) {
                for (Packet piece : packets)
                    writePacketIntl(piece); // Write the individual piece, incrementing its packet
            }
        } else {
            // Ensure the outbound packet's maximum size isn't too large if it can't be split (which it can't here)
            if (packet.getSize() > Fragments.MAXIMUM_PACKET_SIZE)
                throw new IllegalArgumentException("packet too large: " + packet.getSize() + "; " +
                        packet.getHeader().getType().name() + " cannot split");

            // Don't lock here, for the slight performance benefit.
            writePacketIntl(packet);
        }
    }

    private void writePacketIntl(Packet packet) throws IOException, TimeoutException {
        PacketHeader header = packet.getHeader();

        // Count packet
        // depends on the actual packet type coming in
        Pair<Integer, Integer> counter;

        switch (header.getType()) {
            /*case ACK:
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
                break;*/
            case INIT1:
                counter = new Pair<>(101, 0);
                break;
            default:
                counter = localSendCounter.get(header.getType()).next();
                break;
        }

        // Use the counters determined above to generate a packet id and generation id
        header.setPacketId(counter.getKey());
        header.setGeneration(counter.getValue());

        if (header.getType() == PacketBodyType.VOICE) {
            // > X is a ushort in H2N order of an own audio packet counter
            //     it seems it can be the same as the packet counter so we will let the packethandler do it.
            ((PacketBody0Voice)packet.getBody()).setPacketId(header.getPacketId());
        }

        // Flush to a buffer
        ByteBuffer outputBuffer;

        if (!packet.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED)) { // encrypt out
            Ts3Debugging.debug("[PROTOCOL] ENCRYPT " +
                    packet.getHeader().getType().name() + " generation=" +
                    packet.getHeader().getGeneration());

            outputBuffer = getTransformation().encrypt(packet);
        } else { // "fake encrypt"
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

        // Construct actual network packet now that the protocol is finished.
        NetworkPacket networkPacket = new NetworkPacket(
                new DatagramPacket(outputBuffer.array(), outputBuffer.position()),
                packet.getHeader(),
                (ByteBuffer) outputBuffer.position(0)
        );

        // Find if the packet must be acknowledged and create a promise for that action
        boolean willWaitForAck =
                packet.getHeader().getType().getAcknolwedgedBy() != null &&
                        networkPacket.getHeader().getType() != PacketBodyType.PING;

        final PacketResponse response;
        final Map<Integer, PacketResponse> responsibleQueue;

        switch (packet.getHeader().getType()) {
            case COMMAND:
                responsibleQueue = sendQueue;
                break;
            case COMMAND_LOW:
                responsibleQueue = sendQueueLow;
                break;
            case PING:
                responsibleQueue = pingQueue;
                break;
            default:
                responsibleQueue = null;
        }

        if (responsibleQueue != null) {
            responsibleQueue.put(
                    networkPacket.getHeader().getPacketId(),
                    response = new PacketResponse(
                            networkPacket,
                            responsibleQueue,
                            networkPacket.getHeader().getType().canResend() ? 30 : 0
                    )
            );
        } else
            response = null;

        Ts3Debugging.debug("[PROTOCOL] WRITE " + networkPacket.getHeader().getType().name());

        // Real network send
        writeNetworkPacket(networkPacket);

        // Now that the packet is sent, we need to track the stats
        PacketKind kind = networkPacket.getHeader().getType().getKind();
        if (kind != null)
            packetStatistics
                    .get(kind)
                    .processOutgoing(packet);

        // If necessary, fulfill promise for the packet acknowledgement or throw TimeoutException
        if (response != null && willWaitForAck) {
            while (response.getRetries() < response.getMaxTries() &&
                    getState() != ClientConnectionState.DISCONNECTED) {
                try {
                    if (getState() == ClientConnectionState.DISCONNECTED)
                        throw new IllegalStateException("no longer connected");

                    // Wait one cycle for an ACK
                    response.waitForAcknowledgement(1000L);
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

        if (fragment && !networkPacket.getHeader().getType().isSplittable())
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

            PacketTransformation transformation = getTransformation();

            try {
                packetBuffer = ByteBuffer.wrap(
                        transformation.decrypt(
                                networkPacket.getHeader(),
                                packetBuffer,
                                networkPacket.getDatagram().getLength() -
                                        networkPacket.getHeader().getSize()
                        )
                ).order(ByteOrder.BIG_ENDIAN);
            } catch (InvalidCipherTextException e) {
                throw new IOException(
                        "failed to decrypt " + networkPacket.getHeader().getType().name()
                                + " (" +
                                "transformation=" + transformation.getClass() +
                                ", state=" + getState() +
                                ", id=" + packet.getHeader().getPacketId() +
                                ", generation=" + networkPacket.getHeader().getGeneration(),
                        e
                );
            }
        }

        // Fragment handling
        if (networkPacket.getHeader().getType().isSplittable()) {
            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name());

            packet.setBody(new PacketBodyFragment(networkPacket.getHeader().getType(), getRole().getIn()));
        } else {
            Ts3Debugging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name());

            if (packet.getHeader().getPacketFlag(HeaderFlag.COMPRESSED) &&
                    packet.getHeader().getType().isCompressible())
                packet.setBody(new PacketBodyCompressed(networkPacket.getHeader().getType(), getRole().getIn()));
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


        PacketKind kind = networkPacket.getHeader().getType().getKind();
        if (kind != null)
            packetStatistics
                    .get(kind)
                    .processIncoming(packet);

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
            // De-spool spooled-up packets
            if (handlerBacklog.size() > 0) {
                Packet packet = handlerBacklog.remove(0);
                return packet;
            }

            try {
                timeout = getState() == ClientConnectionState.CONNECTED ?
                        Math.min(1000L, 1000L - (System.currentTimeMillis() - lastPing)) :
                        Integer.MAX_VALUE;

                if (timeout <= 0) throw new SocketTimeoutException();

                networkPacket = readNetworkPacket((int) timeout);
            } catch (SocketTimeoutException e) {
                if (getState() == ClientConnectionState.CONNECTED) {
                    if (System.currentTimeMillis() - lastResponse >= TIMEOUT) {
                        Ts3Debugging.debug("Connection timed out.");
                        setState(ClientConnectionState.DISCONNECTED); // Timeout
                    } else {
                        writePacket(new PacketBody4Ping(getRole().getOut()));
                        lastPing = System.currentTimeMillis();
                    }
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
            Packet packet;
            try {
                packet = readPacketIntl(networkPacket);
            } catch (Exception ex) {
                exceptionHandler.accept(
                        new Exception("Problem reading " + networkPacket.getHeader().getType().name(), ex)
                );

                continue;
            }

            // Find if the packet must be acknowledged
            PacketBodyType ackType = packet
                    .getHeader()
                    .getType()
                    .getAcknolwedgedBy();

            if (ackType != null) {
                switch (ackType) {
                    case ACK:
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
                        pong.getHeader().setType(PacketBodyType.PONG);

                        pong.setBody(new PacketBody5Pong(getRole().getOut(), packet.getHeader().getPacketId()));

                        writePacket(pong);

                        continue; // Don't pass pings to the parent
                }
            }

            // Find if the packet is itself an acknowledgement (ACK or ACK_LOW)
            boolean handle;
            final PacketResponse response;
            switch (packet.getHeader().getType()) {
                case ACK:
                    response = sendQueue.get(((PacketBody6Ack)packet.getBody()).getPacketId());
                    if (response == null) {
                        Ts3Debugging.debug("Unrecognized ACK: " + ((PacketBody6Ack) packet.getBody()).getPacketId());
                    } else {
                        lastResponse = System.currentTimeMillis();
                    }

                    handle = false;
                    break;
                case ACK_LOW:
                    response = sendQueueLow.get(((PacketBody7AckLow)packet.getBody()).getPacketId());
                    if (response == null) {
                        Ts3Debugging.debug("Unrecognized ACK_LOW: " + ((PacketBody6Ack) packet.getBody()).getPacketId());
                    } else {
                        lastResponse = System.currentTimeMillis();
                    }

                    handle = false;
                    break;
                case PONG:
                    response = pingQueue.get(((PacketBody5Pong)packet.getBody()).getPacketId());
                    if (response == null) {
                        Ts3Debugging.debug("Unrecognized PONG: " + ((PacketBody5Pong) packet.getBody()).getPacketId());
                    } else {
                        lastResponse = System.currentTimeMillis();
                    }
                    handle = false;
                    break;
                default:
                    handle = true;
                    response = null;
            }

            // Calculate RTT
            if (response != null) {
                long rttNano = packet.getCreatedNanotime() - response.getLastSent();
                double sampleRtt = rttNano / 1_000_000_000D;

                if (packet.getHeader().getType() == PacketBodyType.PONG) {
                    synchronized (pings) {
                        pings.add(sampleRtt); // add rtt

                        while (pings.size() > 5)
                            pings.remove(5 - 1); // rm oldest
                    }
                }

                if (smoothedRtt < 0)
                    smoothedRtt = sampleRtt;
                else
                    smoothedRtt = (long)((1 - alphaSmooth) * smoothedRtt + alphaSmooth * sampleRtt);

                smoothedRttVar = (long)((1 - betaSmooth) * smoothedRttVar + betaSmooth * Math.abs(sampleRtt - smoothedRtt));
                currentRto = smoothedRtt + Math.max(0D, 4 * smoothedRttVar);
            }

            if (response != null)
                response.acknowledge(packet);

            // This is pulled out of the following IF block so that packet generation can increase correctly
            boolean placed = counter != null && counter.put(packet.getHeader().getPacketId());

            if (packet.getHeader().getType().canResend() && packet.getHeader().getType() != PacketBodyType.INIT1) {
                // Find if we already acknowledged this packet
                if (counter != null)
                    handle = handle & placed;
            }

            if (handle) {
                if (packet.getHeader().getType().isSplittable()) {
                    if (!placed) continue; // reset

                    // Place in the reassembly queue
                    PacketReassembly reassembly = reassemblyQueue.get(packet.getHeader().getType());
                    reassembly.put(packet);

                    Packet reassembled;
                    while ((reassembled = reassembly.next()) != null) {
                        Ts3Debugging.debug("[PROTOCOL] REASSEMBLE " + reassembled.getHeader().getType().name() +
                                " id=" + reassembled.getHeader().getPacketId() + " len=" + reassembled.getSize());
                        handlerBacklog.add(reassembled);
                    }
                } else {
                    handlerBacklog.add(packet);
                }
            }
        }

        throw new EOFException();
    }

    /**
     * Gets the ping
     */
    public Pair<Double, Double> getPing() {
        synchronized (pings) {
            if (pings.size() == 1) {
                return new Pair<>(pings.get(0), 0D);
            } else if (pings.size() > 1) {
                double avg = pings.stream().reduce((a, b) -> (a + b) / 2D).orElse(0D);
                double sum = 0;
                int n = 0;
                for (double val : pings) {
                    n ++;
                    sum += (val - avg) * (val - avg);
                }

                double stdDev;
                if (n > 1)
                    stdDev = Math.sqrt(sum / (n - 1));
                else
                    stdDev = 0;

                return new Pair<>(pings.get(0), stdDev);
            } else {
                return new Pair<>((double)TIMEOUT, 0D);
            }
        }
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

    public Consumer<Throwable> getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public Consumer<Packet> getVoiceHandler() {
        return voiceHandler;
    }

    public void setVoiceHandler(Consumer<Packet> voiceHandler) {
        this.voiceHandler = voiceHandler;
    }

    public Consumer<Packet> getWhisperHandler() {
        return whisperHandler;
    }

    public void setWhisperHandler(Consumer<Packet> whisperHandler) {
        this.whisperHandler = whisperHandler;
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
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) continue;

                    getExceptionHandler().accept(e);
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
                } catch (EOFException e) {
                    break;
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        Thread.yield();
                        continue;
                    }

                    exceptionHandler.accept(e);
                    break;
                }
            }

            try {
                if (getState() != ClientConnectionState.DISCONNECTED)
                    setState(ClientConnectionState.DISCONNECTED);
            } catch (Throwable e1) {
                exceptionHandler.accept(e1);
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

        public PacketResponse(NetworkPacket sentPacket,
                              Map<Integer, PacketResponse> responsibleQueue,
                              int maxTries) {
            this.sentPacket = sentPacket;
            this.responsibleQueue = responsibleQueue;
            this.maxTries = maxTries;
            this.lastSent = System.nanoTime();
            this.willResend = maxTries > 0;
        }

        public NetworkPacket getPacket() {
            return sentPacket;
        }

        public void resend() throws IOException {
            AbstractTeamspeakClientSocket.this.writeNetworkPacket(sentPacket);

            long t = System.nanoTime();
            long before = lastSent;
            long elapsed = t - before;

            setLastSent(t);

            double elapsedSeconds = elapsed / 1_000_000_000D;
            currentRto += elapsedSeconds;

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

        int getCurrentPacketId();

        /**
         * Counts the given packet by its packet ID and stores it in history.
         *
         * @param packetId packet to put
         * @return true if the packet was placed, false otherwise
         */
        boolean put(int packetId);

        void reset();
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
        public int getCurrentPacketId() {
            return 0;
        }

        @Override
        public boolean put(int packetId) {
            return true;
        }

        @Override
        public void reset() {
            // Do nothing
        }

    }

    public static class RemoteCounterFull implements RemoteCounter {
        private final Integer[] buffer; // history buffer
        private final int bufferSize; // size of the packet history buffer, used for remembering received packets
        private final int generationSize; // size of the physical window, typically 65536 for a 16-bit/ushort window field

        private Pair<Integer, Integer>
                bufferStart = new Pair<>(0,0), // position of the start of the buffer in the window
                bufferEnd; // position of the end of the buffer in the window

        private int latestPacketId = 0;

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

        public Pair<Integer, Integer> getBufferStart() {
            return bufferStart;
        }

        public Pair<Integer, Integer> getBufferEnd() {
            return bufferEnd;
        }

        @Override
        public int getGeneration(int packetId) {
            Pair<Integer, Integer> bufferStartCopy;
            Pair<Integer, Integer> bufferEndCopy;

            // Shorter synchronization
            synchronized (this) {
                bufferStartCopy = this.bufferStart;
                bufferEndCopy = this.bufferEnd;
            }

            packetId %= generationSize;

            // find if right of the start and within the start's bounds
            if (packetId >= bufferStartCopy.getValue()
                    && packetId < bufferStartCopy.getValue() + bufferSize
                    && packetId < generationSize) {
                return bufferStartCopy.getKey();
            }

            // find if left of end and within the end's bounds
            if (packetId <= bufferEndCopy.getValue()
                    && packetId > 0
                    && packetId > bufferEndCopy.getValue() - bufferSize
                    && packetId < generationSize) {
                return bufferEndCopy.getKey();
            }

            // find if outside of buffer to the right
            if (packetId > bufferEndCopy.getValue())
                return bufferEndCopy.getKey(); // ahead in current generation

            // find if outside of buffer to the left
            if (packetId < bufferStartCopy.getValue())
                return bufferEndCopy.getKey() + 1; // modulated wrap, ahead of current generation

            // should never arrive here.
            throw new IllegalStateException();
        }

        @Override
        public int getCurrentGeneration() {
            return bufferEnd.getKey();
        }

        @Override
        public int getCurrentPacketId() {
            return latestPacketId;
        }

        @Override
        public boolean put(int packetId) {
            synchronized (this) {
                // find if right of the start and within the start's bounds
                if (packetId >= bufferStart.getValue()
                        && packetId < bufferStart.getValue() + bufferSize
                        && packetId < generationSize) {
                    latestPacketId = Math.max(latestPacketId, packetId);

                    return putRelative(packetId - bufferStart.getValue(), bufferStart.getKey());
                }

                // find if left of end and within the end's bounds
                if (packetId <= bufferEnd.getValue()
                        && packetId >= 0
                        && packetId > bufferEnd.getValue() - bufferSize
                        && packetId < generationSize) {
                    latestPacketId = Math.max(latestPacketId, packetId);

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
                    if (toMove > 0 && toMove < bufferSize)
                        System.arraycopy(buffer, amountMoved, buffer, 0, toMove);

                    int toNullify = Math.max(0, Math.min(bufferSize, amountMoved));
                    for (int i = bufferSize - toNullify; i < bufferSize; i++)
                        buffer[i] = null;

                    int bufferStartGeneration = bufferStart.getKey();
                    int bufferStartPosition = bufferStart.getValue();

                    bufferStartPosition += amountMoved;
                    if (bufferStartPosition >= generationSize) {
                        bufferStartPosition %= generationSize;
                        bufferStartGeneration++;
                    }

                    int bufferEndGeneration;
                    int bufferEndPosition = (bufferStartPosition + bufferSize - 1) % generationSize;
                    if (bufferEndPosition < bufferStartPosition) {
                        bufferEndGeneration = bufferStartGeneration + 1;
                    } else
                        bufferEndGeneration = bufferStartGeneration;

                    this.bufferStart = new Pair<>(bufferStartGeneration, bufferStartPosition);
                    this.bufferEnd = new Pair<>(bufferEndGeneration, bufferEndPosition);
                } else
                    throw new IllegalStateException();

                // if we had to adjust the buffer size, recursively retry put
                this.latestPacketId = packetId;

                return put(packetId);
            }
        }

        @Override
        public void reset() {
            synchronized (this) {
                for (int i = 0; i < bufferSize; i++)
                    this.buffer[i] = null;

                this.bufferStart = new Pair<>(0, 0);
                this.bufferEnd = new Pair<>(0, bufferSize - 1);
                this.latestPacketId = 0;
            }
        }

        private boolean putRelative(int index, int generation) {
            synchronized (this) {
                Integer existing = buffer[index];
                if (existing == null || existing != generation) {
                    buffer[index] = generation;
                    return true;
                } else {
                    return false;
                }
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

        default Pair<Integer, Integer> next(int n) {
            Pair<Integer, Integer> last = current();
            for (int i = 0; i < n; i ++) last = next();
            return last;
        }

        int getPacketId();
        boolean setPacketId(int packetId);

        int getGeneration();
        void setGeneration(int i);

        default Pair<Integer,Integer> current() {
            return new Pair<>(getPacketId(), getGeneration());
        }

        void reset();
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

        @Override
        public void reset() {
            // Do nothing
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
                this.generationId = i;
            }
        }

        @Override
        public void reset() {
            synchronized (sendLock) {
                packetId = generationId = 0;
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

    private PacketHandler createHandler(Class<? extends PacketHandler> clazz) throws ReflectiveOperationException {
        return (PacketHandler) ConstructorUtils.invokeConstructor(clazz, this);
    }
}
