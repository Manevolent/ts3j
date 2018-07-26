package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
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
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeoutException;

public abstract class AbstractTeamspeakClientSocket
        extends AbstractTeamspeakSocket
        implements TeamspeakClientSocket {
    private final Object connectionStateLock = new Object();
    private final Map<PacketBodyType, ReassemblyQueue> reassemblyQueue = new LinkedHashMap<>();
    private final NetworkReader networkReader = new NetworkReader();
    private final Map<String, Object> clientOptions = new LinkedHashMap<>();

    private Identity identity = null;
    private ClientConnectionState connectionState = null;
    private PacketTransformation transformation = new DefaultPacketTransformation();
    private PacketHandler handler;

    private Thread networkThread = null;
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
                reassemblyQueue.put(bodyType, new ReassemblyQueue());
    }

    protected void start() {
        Thread networkThread = new Thread(networkReader);
        networkThread.setDaemon(true);
        networkThread.start();
    }

    @Override
    public Map<String, Object> getOptions() {
        return clientOptions;
    }

    public final void setSecureParameters(Ts3Crypt.SecureChannelParameters parameters) {
        setTransformation(new PacketTransformation(parameters.getIvStruct()));
    }

    protected abstract NetworkPacket readNetworkPacket() throws IOException;

    protected abstract void writeNetworkPacket(NetworkPacket packet) throws IOException;

    public void setState(ClientConnectionState state) throws IOException, TimeoutException {
        synchronized (connectionStateLock) {
            if (state != this.connectionState) {
                Ts3Logging.debug("State changing: " + state.name());

                boolean wasDisconnected = this.connectionState == ClientConnectionState.DISCONNECTED;

                if (wasDisconnected) setReading(true);

                this.connectionState = state;

                if (this.connectionState == ClientConnectionState.DISCONNECTED) setReading(false);

                PacketHandler handler = getHandler();

                if (handler != null)
                    ((LocalClientHandler) handler).handleConnectionStateChanging(state);

                if (handler == null || handler.getClass() != state.getHandlerClass()) {
                    Ts3Logging.debug("Assigning " + state.getHandlerClass().getName() + " handler...");

                    setHandler(state.createHandler(this));
                }

                if (this.connectionState == ClientConnectionState.DISCONNECTED)
                    clientOptions.clear();

                connectionStateLock.notifyAll();

                Ts3Logging.debug("State changed: " + state.name());
            }
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
        packet.getHeader().setType(packet.getHeader().getType());

        // Make sure new protocol is set if it must be set
        if (getState() == ClientConnectionState.CONNECTING)
            packet.getHeader().setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);

        // Set header values
        packet.getBody().setHeaderValues(packet.getHeader());

        // Flush to a buffer
        ByteBuffer outputBuffer;

        if (packet.getBody().getType().canEncrypt() &&
                !packet.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED)) {
            outputBuffer = getTransformation().encrypt(packet);
        } else {
            outputBuffer = ByteBuffer.allocate(packet.getSize());

            outputBuffer.order(ByteOrder.BIG_ENDIAN);
            packet.writeHeader(outputBuffer);
            packet.writeBody(outputBuffer);
        }

        Ts3Logging.debug("[PROTOCOL] WRITE " + packet.getHeader().getType().name());

        // Find if the packet must be acknowledged and create a promise for that action
        // TODO

        writeNetworkPacket(new NetworkPacket(
                new DatagramPacket(outputBuffer.array(), outputBuffer.position()),
                packet.getHeader(),
                (ByteBuffer) outputBuffer.position(0)
        ));

        // If necessary, fulfill promise for the packet acknowledgement or throw TimeoutException
        // TODO
    }

    private Packet readPacketIntl() throws IOException {
        NetworkPacket networkPacket = readNetworkPacket();

        if (networkPacket.getHeader() == null) throw new NullPointerException("header");

        Packet packet = new Packet(networkPacket.getHeader().getRole());
        packet.setHeader(networkPacket.getHeader());

        boolean fragment = networkPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED);

        if (networkPacket.getHeader().getType().isSplittable()) {
            if (fragment)
                throw new IllegalArgumentException("packet is fragment, but not splittable");
            else {
                fragment = reassemblyQueue.get(networkPacket.getHeader().getType()).isReassembling();
            }
        }

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

        // Fragment handling
        if (fragment) {
            Ts3Logging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name() + " fragment");

            packet.setBody(
                    new PacketBodyFragment(
                            networkPacket.getHeader().getType(),
                            getRole().getIn()
                    )
            );
        } else {
            Ts3Logging.debug("[PROTOCOL] READ " + networkPacket.getHeader().getType().name());
        }

        // Compression?
        // TODO

        // Read packet body
        packet.readBody(packetBuffer);

        return packet;
    }

    @Override
    public Packet readPacket() throws IOException, TimeoutException {
        while (true) {
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

            // Find if the packet is itself an acknowledgement
            switch (packet.getHeader().getType()) {
                case ACK:
                    break;
                case ACK_LOW:
                    break;
                default:
                    getHandler().handlePacket(packet);
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
            } else {
                start();
            }
        }
    }

    private class NetworkReader implements Runnable {
        @Override
        public void run() {
            while (isReading()) {
                try {
                    readPacket();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) continue;

                    Ts3Logging.debug("Problem handling packet", e);
                }
            }
        }
    }

    private class ReassemblyQueue {
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

                boolean fragmentationEnded = oldState;

                queue.put(packet.getHeader().getPacketId(), packet);

                if (fragmentationEnded)
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
