package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.enums.ConnectionState;
import com.github.manevolent.ts3j.enums.PacketFlags;
import com.github.manevolent.ts3j.enums.PacketType;
import com.github.manevolent.ts3j.handler.TeamspeakClientHandler;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.TimeoutException;

public class TeamspeakClient {
    private final PacketReceiver packetReceiver = new PacketReceiver();

    private final DatagramSocket datagramSocket;
    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[1500], 1500);

    private final Object connectionStateLock = new Object();

    private TeamspeakClientHandler handler;

    private InetSocketAddress remote = null;
    private ConnectionState connectionState = null;
    private int returnCodeIndex = 0;

    public TeamspeakClient(DatagramSocket datagramSocket) {
        this.datagramSocket = datagramSocket;
    }

    public TeamspeakClient() throws SocketException {
        this(new DatagramSocket());

        setConnectionState(ConnectionState.DISCONNECTED);
    }

    protected void setConnectionState(ConnectionState state) {
        synchronized (connectionStateLock) {
            if (state != this.connectionState) {
                boolean wasDisconnected = this.connectionState == ConnectionState.DISCONNECTED;

                this.connectionState = state;

                if (wasDisconnected) {
                    Ts3Logging.debug("Starting network receiver thread...");

                    Thread receiverThread = new Thread(new PacketReceiver());
                    receiverThread.setDaemon(true);
                    receiverThread.start();
                }

                if (handler != null)
                    handler.handleConnectionStateChanging(state);

                if (handler == null || handler.getClass() != state.getHandlerClass()) {
                    Ts3Logging.debug("Assigning " + state.getHandlerClass().getName() + " handler...");

                    handler = state.createHandler(this);

                    try {
                        handler.onAssigned();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                connectionStateLock.notifyAll();
            }
        }
    }

    public boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    private void joinConnectionState(ConnectionState state, long wait)
            throws InterruptedException, TimeoutException {
        synchronized (connectionStateLock) {
            if (connectionState != state) {
                connectionStateLock.wait(wait);

                if (connectionState != state)
                    throw new TimeoutException("timeout waiting for " + state.name() + " state");
            }
        }
    }

    public CommandResponse sendCommand(Command command) throws IOException {
        if (command.willExpectResponse() && !isConnected())
            throw new IOException("not connected");

        CommandResponse response;

        if (command.willExpectResponse()) {
            int returnCodeIndex = ++ this.returnCodeIndex;
            command.appendParameter(new CommandSingleParameter("return_code", returnCodeIndex));
            response = new CommandResponse(command, returnCodeIndex);
        } else {
            response = new CommandResponse(command, -1);
        }

        String commandString = command.toString();

        response.setDispatchedTime(System.currentTimeMillis());

        return response;
    }

    public void connect(InetSocketAddress remote, String password, long timeout) throws IOException {
        try {
            if (connectionState != ConnectionState.DISCONNECTED)
                throw new IllegalStateException(connectionState.name());

            datagramSocket.connect(remote);

            setConnectionState(ConnectionState.CONNECTING);
            joinConnectionState(ConnectionState.CONNECTED, timeout);
        } catch (Throwable e) {
            setConnectionState(ConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }

    public void send(PacketType type, byte[] data) throws IOException {
        C2SPacket packet = createOutgoingPacket(type, data);
    }

    private C2SPacket createOutgoingPacket(PacketType type, byte[] data) throws IOException {
        C2SPacket packet = new C2SPacket();

        //TODO
        packet.setPacketId(0);

        //TODO
        packet.setGenerationId(0);

        //TODO
        packet.setClientId(0);

        //TODO
        packet.packetFlags |= 0;

        switch (type) {
            case VOICE:
            case VOICE_WHISPER:

                break;
            case COMMAND:
            case COMMAND_LOW:

                break;
            case PING:

                break;
            case PONG:

                break;
            case ACK:
            case ACK_LOW:

                break;
            case INIT:
                packet.setPacketFlags(packet.getPacketFlags() | PacketFlags.UNENCRYPTED.getIndex());
                break;
        }

        return packet;
    }

    /**
     * Wire packet write
     * @param packet Packet to write
     * @throws IOException
     */
    private void sendNetworkPacket(DatagramPacket packet) throws IOException {
        Ts3Logging.debug(
                "[NETWORK] WRITE Len=" + packet.getLength() + " to " + datagramSocket.getRemoteSocketAddress() +
                "\n" + Ts3Logging.getHex(packet.getData())
        );

        packet.setSocketAddress(datagramSocket.getRemoteSocketAddress());

        datagramSocket.send(packet);
    }

    private DatagramPacket receiveNetworkPacket() throws IOException {
        datagramSocket.receive(datagramPacket);
        return datagramPacket;
    }

    private void handleNetworkPacket(DatagramPacket packet) {
        Ts3Logging.debug(
                "[NETWORK] READ Len=" + packet.getLength() + " from " + packet.getSocketAddress() +
                "\n" + Ts3Logging.getHex(packet.getData())
        );

        if (handler != null) handler.handleNetworkPacket(packet);
    }

    public InetSocketAddress getRemote() {
        return remote;
    }

    private class PacketReceiver implements Runnable {
        @Override
        public void run() {
            while (connectionState != ConnectionState.DISCONNECTED) {
                try {
                    handleNetworkPacket(receiveNetworkPacket());
                } catch (IOException e) {
                    Ts3Logging.info("Problem handling packet: " + e.getMessage());
                }
            }

            Ts3Logging.debug("Leaving packet receiver");
        }
    }

    private class C2SPacket {
        private int packetId;
        private int generationId;
        private int clientId;
        private int packetFlags;

        private PacketType type;
        private byte[] data;
        private Date lastSendTime;

        public PacketType getType() {
            return type;
        }

        public void setType(PacketType type) {
            this.type = type;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public Date getLastSendTime() {
            return lastSendTime;
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

        public int getClientId() {
            return clientId;
        }

        public void setClientId(int clientId) {
            this.clientId = clientId;
        }

        public int getPacketFlags() {
            return packetFlags;
        }

        public void setPacketFlags(int packetFlags) {
            this.packetFlags = packetFlags;
        }

        /**
         * Sends, or re-sends, a packet
         */
        public void send() {

        }
    }
}
