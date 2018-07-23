package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.handler.TeamspeakClientHandler;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.Teamspeak3Socket;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class Teamspeak3Client {
    private final PacketReceiver packetReceiver = new PacketReceiver();

    private final Teamspeak3Socket socket;
    private final Object connectionStateLock = new Object();

    private final Map<String, Object> clientOptions = new LinkedHashMap<>();

    private TeamspeakClientHandler handler;

    private InetSocketAddress remote = null;
    private ClientConnectionState clientConnectionState = null;
    private int returnCodeIndex = 0;

    public Teamspeak3Client(DatagramSocket datagramSocket) throws SocketException {
        this.socket = new Teamspeak3Socket(SocketRole.CLIENT, datagramSocket);

        setClientConnectionState(ClientConnectionState.DISCONNECTED);
    }

    public Teamspeak3Client() throws SocketException {
        this(new DatagramSocket());
    }

    public InetSocketAddress getRemote() {
        return remote;
    }

    public void send(Packet packet) throws IOException {
        // Construct header
        ClientPacketHeader header = new ClientPacketHeader();

        packet.setHeaderValues(header);

        socket.send(header, packet);
    }

    protected void setClientConnectionState(ClientConnectionState state) {
        synchronized (connectionStateLock) {
            if (state != this.clientConnectionState) {
                Ts3Logging.debug("State changing: " + state.name());

                boolean wasDisconnected = this.clientConnectionState == ClientConnectionState.DISCONNECTED;

                this.clientConnectionState = state;

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

    public <T extends Object> T getOption(String key, Class<T> clazz) {
        Object value = clientOptions.get(key);

        if (value == null) return (T) null;

        else if (!value.getClass().isAssignableFrom(clazz)) {
            throw new ClassCastException(
                    clazz.getName()
                    + " is not assignable to option type " +
                    value.getClass().getName()
            );
        } else
            return (T) value;
    }

    public Object setOption(String key, Object value) {
        return clientOptions.put(key, value);
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
            if (clientConnectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(clientConnectionState.name());

            setOption("client.hostname", remote.getHostString());
            setOption("client.password", password);

            socket.connect(remote);

            setClientConnectionState(ClientConnectionState.CONNECTING);
            joinConnectionState(ClientConnectionState.CONNECTED, timeout);
        } catch (Throwable e) {
            setClientConnectionState(ClientConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }

    private void handlePacket(NetworkPacket packet) throws Exception {
        if (handler != null) handler.handlePacket(packet);
    }

    private class PacketReceiver implements Runnable {
        @Override
        public void run() {
            Ts3Logging.debug("Entering packet receiver");

            while (clientConnectionState != ClientConnectionState.DISCONNECTED) {
                try {
                    handlePacket(socket.receive());
                } catch (Exception e) {
                    Ts3Logging.debug("Problem handling packet", e);
                }
            }

            Ts3Logging.debug("Leaving packet receiver");
        }
    }
}
