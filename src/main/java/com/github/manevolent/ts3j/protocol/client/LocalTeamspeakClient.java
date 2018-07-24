package com.github.manevolent.ts3j.protocol.client;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.LocalEndpoint;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.socket.LocalTeamspeakSocket;
import com.github.manevolent.ts3j.protocol.packet.Packet;
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

    private LocalClientHandler handler;
    private ClientConnectionState clientConnectionState = null;

    private int returnCodeIndex = 0;

    public LocalTeamspeakClient(LocalTeamspeakSocket socket) {
        super(socket);

        if (socket.getSocketRole() != SocketRole.CLIENT)
            throw new IllegalArgumentException("invalid socket role: " + socket.getSocketRole().name());

        setClientConnectionState(ClientConnectionState.DISCONNECTED);
    }

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

    @Override
    public SocketRole getRole() {
        return SocketRole.CLIENT;
    }

    public void send(Packet packet) throws IOException {
        // Construct header
        ClientPacketHeader header = new ClientPacketHeader();

        packet.setHeaderValues(header);

        getSocket().send(header, packet);
    }

    protected void setClientConnectionState(ClientConnectionState state) {
        synchronized (connectionStateLock) {
            if (state != this.clientConnectionState) {
                Ts3Logging.debug("State changing: " + state.name());

                boolean wasDisconnected = this.clientConnectionState == ClientConnectionState.DISCONNECTED;

                this.clientConnectionState = state;

                if (wasDisconnected)
                    setRunning(true);

                if (handler != null)
                    handler.handleConnectionStateChanging(state);

                if (handler == null || handler.getClass() != state.getHandlerClass()) {
                    Ts3Logging.debug("Assigning " + state.getHandlerClass().getName() + " handler...");

                    handler = state.createHandler(this);

                    try {
                        setHandler(handler);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }

                if (this.clientConnectionState == ClientConnectionState.DISCONNECTED)
                    clientOptions.clear();

                connectionStateLock.notifyAll();

                Ts3Logging.debug("State changed: " + state.name());
            }
        }
    }

    public boolean isConnected() {
        return clientConnectionState == ClientConnectionState.CONNECTED;
    }

    @Override
    public Map<String, Object> getOptions() {
        return clientOptions;
    }

    public ClientConnectionState getClientConnectionState() {
        return clientConnectionState;
    }

    private void joinConnectionState(ClientConnectionState state, long wait)
            throws InterruptedException, TimeoutException {
        synchronized (connectionStateLock) {
            long start = System.currentTimeMillis();

            while (clientConnectionState != state && System.currentTimeMillis() - start < wait) {
                connectionStateLock.wait(Math.max(0L, wait - (System.currentTimeMillis() - start)));
            }

            if (clientConnectionState != state)
                throw new TimeoutException("timeout waiting for " + state.name() + " state");
        }
    }

    public void connect(InetSocketAddress remote, String password, long timeout) throws IOException {
        try {
            if (clientConnectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(clientConnectionState.name());

            setOption("client.hostname", remote.getHostString());
            setOption("client.password", password);

            getSocket().connect(remote);

            setClientConnectionState(ClientConnectionState.CONNECTING);
            joinConnectionState(ClientConnectionState.CONNECTED, timeout);
        } catch (Throwable e) {
            setClientConnectionState(ClientConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }
}
