package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.api.Channel;
import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.AbstractCommandResponse;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.event.TS3Event;
import com.github.manevolent.ts3j.event.TS3Listener;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class LocalTeamspeakClientSocket
        extends AbstractTeamspeakClientSocket
        implements CommandProcessor {
    private final DatagramSocket socket;

    {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private final DatagramPacket packet = new DatagramPacket(new byte[500], 500);

    private final Map<String, CommandProcessor> namedProcessors = new HashMap<>();
    private final Object commandSendLock = new Object();

    private List<TS3Listener> listeners = new LinkedList<>();

    private int acceptingReturnCode = 0;
    private int lastReturnCode = 0;
    private Map<Integer, CommandProcessor> awaitingCommands = new HashMap<>();

    public LocalTeamspeakClientSocket() {
        super();

        namedProcessors.put("initserver", new InitServerHandler());
        namedProcessors.put("channellist", new NullRouteHandler());
        namedProcessors.put("channellistfinished", new ChannelListFinishedHandler());
    }

    public void addListener(TS3Listener listener) {
        this.listeners.add(listener);
    }

    public boolean removeListener(TS3Listener listener) {
        return this.listeners.remove(listener);
    }

    @Override
    protected NetworkPacket readNetworkPacket() throws IOException {
        socket.receive(packet);

        Ts3Debugging.debug("[NETWORK] READ len=" + packet.getLength() + " from " + packet.getSocketAddress());

        PacketHeader header;

        try {
            header = getRole().getIn().getHeaderClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(packet.getData(), packet.getOffset(), packet.getLength())
                .order(ByteOrder.BIG_ENDIAN);

        header.read(buffer);

        return new NetworkPacket(
                packet,
                header,
                buffer
        );
    }

    @Override
    protected void writeNetworkPacket(NetworkPacket packet) throws IOException {
        Ts3Debugging.debug("[NETWORK] WRITE id=" + packet.getHeader().getPacketId() + " len=" +
                packet.getDatagram().getLength()
                + " to " +
                socket.getRemoteSocketAddress());

        socket.send(packet.getDatagram());
    }


    @Override
    public LocalIdentity getIdentity() {
        return (LocalIdentity) super.getIdentity();
    }

    public void setIdentity(LocalIdentity identity) {
        super.setIdentity(identity);
    }

    /**
     * Initiates a connection to a server
     * @param remote remote sever to contact
     * @param password server password
     * @param timeout timeout, in milliseconds, to complete a connection.
     * @throws IOException
     */
    public void connect(InetSocketAddress remote, String password, long timeout)
            throws IOException, TimeoutException {
        try {
            Ts3Debugging.debug("Connecting to " + remote + "...");

            ClientConnectionState connectionState = getState();

            if (connectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(connectionState.name());

            setClientId(0);
            setPacketId(0);
            setGenerationId(0);

            setOption("client.hostname", remote.getHostString());

            if (password != null) setOption("client.password", password);

            socket.connect(remote);

            setState(ClientConnectionState.CONNECTING);

            waitForState(ClientConnectionState.CONNECTED, timeout);
        } catch (TimeoutException e) {
            setState(ClientConnectionState.DISCONNECTED);

            throw e;
        } catch (Throwable e) {
            setState(ClientConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }

    @Override
    protected boolean isReading() {
        return socket.isConnected();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public void process(AbstractTeamspeakClientSocket client,
                        SingleCommand singleCommand)
            throws CommandProcessException {
        CommandProcessor processor = namedProcessors.get(singleCommand.getName());
        if (processor != null) {
            processor.process(client, singleCommand);
        } else if (singleCommand.getName().startsWith("notify")) {
            TS3Event event = TS3Event.createEvent(singleCommand);
            for (TS3Listener listener : listeners) event.fire(listener);
        } else {
            CommandProcessor awaitingCommandProcessor;

            synchronized (commandSendLock) {
                awaitingCommandProcessor = awaitingCommands.get(acceptingReturnCode);
            }

            if (awaitingCommandProcessor != null)
                awaitingCommandProcessor.process(client, singleCommand);
        }
    }

    public CommandResponse<Iterable<Channel>> getChannels() throws IOException, TimeoutException {
        return executeMappedCommand(
                new SingleCommand("channellist", ProtocolRole.CLIENT),
                command -> new Channel(command.toMap())
        );
    }

    private CommandResponse<Iterable<MultiCommand>> executeCommand(Command command)
            throws IOException, TimeoutException {
        return executeCommand(command, Function.identity());
    }

    private <T> CommandResponse<T> executeCommand(
            Command command,
            Function<Iterable<MultiCommand>, T> processor
    ) throws IOException, TimeoutException {
        int returnCode;

        ClientCommandResponse<T> response;

        // sync may not be necessary here
        synchronized (commandSendLock) {
            returnCode = lastReturnCode++;
            awaitingCommands.put(returnCode, response = new ClientCommandResponse<>(returnCode, processor));
        }

        command.add(new CommandSingleParameter("return_code", Integer.toString(returnCode)));

        try {
            writePacket(new PacketBody2Command(getRole().getOut(), command));
        } catch (Throwable e) {
            awaitingCommands.remove(returnCode);

            throw e;
        }

        return response;
    }

    private <T> CommandResponse<Iterable<T>> executeMappedCommand(
            Command command,
            Function<SingleCommand, T> processor
    ) throws IOException, TimeoutException {
        int returnCode;

        // sync may not be necessary here
        synchronized (commandSendLock) {
            returnCode = lastReturnCode++;
        }

        command.add(new CommandSingleParameter("return_code", Integer.toString(returnCode)));

        ClientCommandResponse<Iterable<T>> response;

        awaitingCommands.put(returnCode, response = new ClientCommandResponse<>(returnCode, multiCommands -> {
            List<SingleCommand> commands = new LinkedList<>();
            for (MultiCommand multiCommand : multiCommands) commands.addAll(multiCommand.simplify());

            List<T> results = new LinkedList<>();
            for (SingleCommand command1 : commands)
                results.add(processor.apply(command1));

            return results;
        }));

        try {
            writePacket(new PacketBody2Command(getRole().getOut(), command));
        } catch (Throwable e) {
            awaitingCommands.remove(returnCode);

            throw e;
        }

        return response;
    }

    private class ClientCommandResponse<T>
            extends AbstractCommandResponse<T>
            implements CommandProcessor {
        private final int returnCode;
        private final List<MultiCommand> commands = new LinkedList<>();

        public ClientCommandResponse(int returnCode,
                                     Function<Iterable<MultiCommand>, T> processor) {
            super(processor);
            this.returnCode = returnCode;
        }

        @Override
        public void process(AbstractTeamspeakClientSocket client,
                             MultiCommand multiCommand)
                throws CommandProcessException {
            if (multiCommand.getName().equalsIgnoreCase("error")) {
                SingleCommand command = multiCommand.simplifyFirst();
                int errorId = Integer.parseInt(command.get("id").getValue());

                switch (errorId) {
                    case 0:
                        completeSuccess(commands);
                        break;
                    default:
                        completeFailure(new CommandException(command.get("msg").getValue(), errorId));
                        break;
                }

                synchronized (awaitingCommands) {
                    awaitingCommands.remove(returnCode);
                }
            } else
                commands.add(multiCommand);
        }

        public int getReturnCode() {
            return returnCode;
        }
    }

    private class ChannelListFinishedHandler implements CommandProcessor {

    }

    private class NullRouteHandler implements CommandProcessor {
        @Override
        public void process(AbstractTeamspeakClientSocket client, SingleCommand singleCommand)
                throws CommandProcessException {
            // Do nothing
        }
    }

    private class InitServerHandler implements CommandProcessor {
        @Override
        public void process(AbstractTeamspeakClientSocket client, SingleCommand singleCommand)
                throws CommandProcessException {
            try {
                setState(ClientConnectionState.CONNECTED);
            } catch (Exception e) {
                throw new CommandProcessException(e);
            }
        }
    }
}
