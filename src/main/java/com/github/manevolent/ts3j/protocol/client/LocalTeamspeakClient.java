package com.github.manevolent.ts3j.protocol.client;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.protocol.LocalEndpoint;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.Packet6Ack;
import com.github.manevolent.ts3j.protocol.packet.PacketType;
import com.github.manevolent.ts3j.protocol.packet.channel.PacketChannel;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.LocalTeamspeakSocket;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class LocalTeamspeakClient extends LocalEndpoint implements TeamspeakClient {
    private final Object connectionStateLock = new Object();
    private final Map<String, Object> clientOptions = new LinkedHashMap<>();
    private final PacketChannel channel = new PacketChannel();

    private ClientConnectionState connectionState = null;

    private int packetId = 0;
    private int clientId = 0;
    private int returnCodeIndex = 0;

    public LocalTeamspeakClient(LocalTeamspeakSocket socket) {
        super(socket);

        if (socket.getSocketRole() != SocketRole.CLIENT)
            throw new IllegalArgumentException("invalid socket role: " + socket.getSocketRole().name());

        setConnectionState(ClientConnectionState.DISCONNECTED);
    }

    @Override
    public SocketRole getRole() {
        return SocketRole.CLIENT;
    }

    @Override
    protected void acknowledge(NetworkPacket packet, PacketType ackType) throws IOException {
        switch (ackType) {
            case ACK:
                Ts3Logging.debug("ACK " + packet.getHeader().getType().name());

                Packet6Ack ack = new Packet6Ack(ProtocolRole.CLIENT);
                ack.setPacketId(packet.getHeader().getPacketId());
                send(ack);

                break;
        }
    }

    // Just return the client's single channel (to the server) here
    @Override
    protected PacketChannel getChannel(NetworkPacket packet) {
        return channel;
    }

    @Override
    public Map<String, Object> getOptions() {
        return clientOptions;
    }

    public void setSecureParameters(Ts3Crypt.SecureChannelParameters parameters) {
        getSocket().setPacketTransformation(new PacketTransformation(parameters.getIvStruct()));
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
            int returnCodeIndex = ++ this.returnCodeIndex;
            //command.appendParameter(new CommandSingleParameter("return_code", returnCodeIndex));
            response = new CommandResponse(command, returnCodeIndex);
        } else {
            response = new CommandResponse(command, -1);
        }

        String commandString = command.toString();

        response.setDispatchedTime(System.currentTimeMillis());

        return response;
    }

    public LocalTeamspeakClient() throws SocketException {
        this(new LocalTeamspeakSocket(SocketRole.CLIENT));
    }

    /**
     * Sends a packet to the server
     * @param packet Packet to send
     * @throws IOException
     */
    int z = 0;
    public void send(Packet packet) throws IOException {
        // Construct header
        ClientPacketHeader header = new ClientPacketHeader();

        header.setClientId(clientId);
        header.setPacketId(packetId);

        packet.setHeaderValues(header);

        if (connectionState == ClientConnectionState.CONNECTING) {
            header.setPacketId(z++);
            header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
        }

        getSocket().send(header, packet);
    }

    public void setConnectionState(ClientConnectionState state) {
        synchronized (connectionStateLock) {
            if (state != this.connectionState) {
                Ts3Logging.debug("State changing: " + state.name());

                boolean wasDisconnected = this.connectionState == ClientConnectionState.DISCONNECTED;

                this.connectionState = state;

                if (wasDisconnected)
                    setRunning(true);

                if (channel.getHandler() != null)
                    ((LocalClientHandler)channel.getHandler()).handleConnectionStateChanging(state);

                if (channel.getHandler() == null || channel.getHandler().getClass() != state.getHandlerClass()) {
                    Ts3Logging.debug("Assigning " + state.getHandlerClass().getName() + " handler...");

                    try {
                        channel.setHandler(state.createHandler(this));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                if (this.connectionState == ClientConnectionState.DISCONNECTED)
                    clientOptions.clear();

                connectionStateLock.notifyAll();

                Ts3Logging.debug("State changed: " + state.name());
            }
        }
    }

    public ClientConnectionState getConnectionState() {
        return connectionState;
    }

    private void joinConnectionState(ClientConnectionState state, long wait)
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
     * Initiates a connection to a server
     * @param remote remote sever to contact
     * @param password server password
     * @param timeout timeout, in milliseconds, to complete a connection.
     * @throws IOException
     */
    public void connect(InetSocketAddress remote, String password, long timeout) throws IOException {
        try {
            if (connectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(connectionState.name());

            setOption("client.hostname", remote.getHostString());
            setOption("client.password", password);

            getSocket().connect(remote);

            setConnectionState(ClientConnectionState.CONNECTING);
            joinConnectionState(ClientConnectionState.CONNECTED, timeout);
        } catch (Throwable e) {
            setConnectionState(ClientConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }
}
