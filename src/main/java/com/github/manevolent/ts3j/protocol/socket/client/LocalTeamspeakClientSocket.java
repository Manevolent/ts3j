package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.api.Ban;
import com.github.manevolent.ts3j.api.Channel;
import com.github.manevolent.ts3j.api.Client;
import com.github.manevolent.ts3j.api.Permission;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.AbstractCommandResponse;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.event.TS3Event;
import com.github.manevolent.ts3j.event.TS3Listener;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.identity.Uid;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.util.HighPrecisionRecurrentTask;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    private Microphone microphone;
    private Thread microphoneThread;

    private List<TS3Listener> listeners = new LinkedList<>();

    private int acceptingReturnCode = 0;
    private int lastReturnCode = 0;
    private Map<Integer, CommandProcessor> awaitingCommands = new ConcurrentHashMap<>();

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
    protected NetworkPacket readNetworkPacket(int timeout) throws
            IOException,
            SocketTimeoutException {

        socket.setSoTimeout(timeout);
        socket.receive(packet);

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

        Ts3Debugging.debug("[NETWORK] READ" +
                " id=" + header.getPacketId() +
                " len=" + packet.getLength() +
                " from " + packet.getSocketAddress());

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

            microphoneThread = new HighPrecisionRecurrentTask(
                    1000,
                    0.020F,
                    new MicrophoneTask()
            );
            microphoneThread.setDaemon(true);
            microphoneThread.start();
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

        final ClientCommandResponse<T> response;

        // sync may not be necessary here
        synchronized (commandSendLock) {
            returnCode = lastReturnCode++;
            awaitingCommands.put(returnCode, response = new ClientCommandResponse<>(returnCode, processor));
            acceptingReturnCode = calculateAcceptingReturnCode();
        }

        try {
            command.add(new CommandSingleParameter("return_code", Integer.toString(returnCode)));

            writePacket(new PacketBody2Command(getRole().getOut(), command));
        } catch (Throwable e) {
            synchronized (commandSendLock) {
                awaitingCommands.remove(returnCode);
                acceptingReturnCode = calculateAcceptingReturnCode();
            }

            throw e;
        }

        return response;
    }

    private <T> CommandResponse<Iterable<T>> executeMappedCommand(
            Command command,
            Function<SingleCommand, T> processor
    ) throws IOException, TimeoutException {
        int returnCode;
        final ClientCommandResponse<Iterable<T>> response;

        // sync may not be necessary here
        synchronized (commandSendLock) {
            returnCode = lastReturnCode++;

            command.add(new CommandSingleParameter("return_code", Integer.toString(returnCode)));

            awaitingCommands.put(returnCode, response = new ClientCommandResponse<>(returnCode, multiCommands -> {
                List<SingleCommand> commands = new LinkedList<>();
                for (MultiCommand multiCommand : multiCommands) commands.addAll(multiCommand.simplify());

                List<T> results = new LinkedList<>();
                for (SingleCommand command1 : commands)
                    results.add(processor.apply(command1));

                return results;
            }));

            acceptingReturnCode = calculateAcceptingReturnCode();
        }

        try {
            writePacket(new PacketBody2Command(getRole().getOut(), command));
        } catch (Throwable e) {
            synchronized (commandSendLock) {
                awaitingCommands.remove(returnCode);

                acceptingReturnCode = calculateAcceptingReturnCode();
            }

            throw e;
        }

        return response;
    }

    public Microphone getMicrophone() {
        return microphone;
    }

    public void setMicrophone(Microphone microphone) {
        if (this.microphone != microphone) {
            this.microphone = microphone;

            if (microphone == null && microphoneThread != null) {
                microphoneThread.interrupt();
                microphoneThread = null;
            }
        }
    }

    private int calculateAcceptingReturnCode() {
        int lowest = Integer.MAX_VALUE;
        for (Integer integer : awaitingCommands.keySet()) lowest = Math.min(lowest, integer);

        return lowest;
    }

    private class MicrophoneTask implements Runnable {
        @Override
        public void run() {
            PacketBody0Voice voice = new PacketBody0Voice(getRole().getOut());
            Microphone microphone;
            while (isConnected()) {
                // Attempt to get the current microphone instance, saving it in a variable to prevent
                // any race conditions by accessing the parent class' this.microphone.
                if ((microphone = LocalTeamspeakClientSocket.this.microphone) == null)
                    continue;

                if (microphone.isMuted()) {
                    // TODO
                    // Set a state and send a muted event
                } else if (microphone.isReady()) {
                    voice.setCodecType(microphone.getCodec());
                    voice.setCodecData(microphone.provide());

                    try {
                        writePacket(voice);
                    } catch (IOException e) {
                        // All we are really concerned about here is a disconnection event, in which case the loop
                        // will exit.
                    } catch (TimeoutException e) {
                        // Not really possible, as we might expect, voice doesn't have ACKs.
                    }
                }
            }
        }
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
            if (multiCommand.getName().equalsIgnoreCase("error"))
               handleError(multiCommand.simplifyOne());
            else
                commands.add(multiCommand);
        }

        @Override
        public void process(AbstractTeamspeakClientSocket client,
                            SingleCommand command)
                throws CommandProcessException {
            if (command.getName().equalsIgnoreCase("error"))
                handleError(command);
            else
                commands.add(new MultiCommand(command.getName(), ProtocolRole.CLIENT, command));
        }

        private void handleError(SingleCommand command) {
            int errorId = Integer.parseInt(command.get("id").getValue());

            synchronized (awaitingCommands) {
                awaitingCommands.remove(returnCode);
                acceptingReturnCode = calculateAcceptingReturnCode();
            }

            switch (errorId) {
                case 0:
                    completeSuccess(commands);
                    break;
                default:
                    completeFailure(new CommandException(command.get("msg").getValue(), errorId));
                    break;
            }
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
                setClientId(Integer.parseInt(singleCommand.get("aclid").getValue()));

                setState(ClientConnectionState.CONNECTED);
            } catch (Exception e) {
                throw new CommandProcessException(e);
            }
        }
    }

    /**
     * Commands
     */

    public void addBan(String ip, String name, Uid uid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banadd", ProtocolRole.CLIENT);

        if (ip != null) banCommand.add(new CommandSingleParameter("ip", ip));
        if (name != null) banCommand.add(new CommandSingleParameter("name", name));
        if (uid != null) banCommand.add(new CommandSingleParameter("uid", uid.toBase64()));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        executeCommand(banCommand).complete();
    }

    public Iterable<Ban> banClient(int clid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banclient", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("clid", Integer.toString(clid)));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        return executeMappedCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public Iterable<Ban> banDatabaseClient(int dbid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banclient", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("cldbid", Integer.toString(dbid)));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        return executeMappedCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public Iterable<Ban> banClient(Uid uid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banclient", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("uid", uid.toBase64()));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        return executeMappedCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public Iterable<Ban> banClient(Client client, Integer timeInSeconds, String reason)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        return banClient(client.getId(), timeInSeconds, reason);
    }

    public void deleteBan(int banId)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("deleteban", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("banid", Integer.toString(banId)));

        executeCommand(banCommand).complete();
    }

    public void deleteAllBans()
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        executeCommand(new SingleCommand("bandelall", ProtocolRole.CLIENT)).complete();
    }

    // TODO notifybanlist is returned instead
    public Iterable<Ban> listBans()
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        Command banCommand = new SingleCommand("banlist", ProtocolRole.CLIENT);
        return executeMappedCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public void deleteBan(Ban ban)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        deleteBan(ban.getId());
    }

    public CommandResponse<Iterable<MultiCommand>> channelAddPermission(int channelId, Permission... permission)
            throws IOException, TimeoutException {
        MultiCommand command = new MultiCommand(
                "channeladdperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channeladdperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue()))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));

        return executeCommand(command);
    }

    public CommandResponse<Iterable<MultiCommand>> channelClientAddPermission(int channelId,
                                                                              int clientDatabaseId,
                                                                              Permission... permission)
            throws IOException, TimeoutException {
        MultiCommand command = new MultiCommand(
                "channelclientaddperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channelclientaddperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue()))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("cldbid", Integer.toString(channelId)));

        return executeCommand(command);
    }

    public void channelClientDeletePermission(int channelId,
                                                                              int clientDatabaseId,
                                                                              Permission... permission)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        MultiCommand command = new MultiCommand(
                "channeldelperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channeldelperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue()))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("cldbid", Integer.toString(channelId)));

        executeCommand(command).complete();
    }

    /**
     * Displays a list of clients that are in the channel specified by the cid
     parameter. Included information is the clientID, client database id, nickname,
     channelID and client type.
     Please take note that you can only view clients in channels that you are
     currently subscribed to.

     Here is a list of the additional display paramters you will receive for
     each of the possible modifier parameters.

     * @param channelId channel ID to look at
     * @return list of channel clients
     * @throws IOException
     * @throws TimeoutException
     */
    // TODO command not found
    public Iterable<Client> listChannelClients(int channelId)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        Command command = new SingleCommand("channelclientlist", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));

        return executeMappedCommand(command, x -> new Client(x.toMap())).get();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public void clientMove(int clientId, int channelId, String password)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("clientmove", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));
        if (password != null) command.add(new CommandSingleParameter("cpw", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("clid", Integer.toString(clientId)));

        executeCommand(command).complete();
    }

    public void joinChannel(int channelId, String password)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        clientMove(getClientId(), channelId, password);
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public Iterable<Channel> listChannels()
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("channellist", ProtocolRole.CLIENT);

        return executeMappedCommand(command, x -> new Channel(x.toMap())).get();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public Iterable<Client> listClients()
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("clientlist", ProtocolRole.CLIENT);

        return executeMappedCommand(command, x -> new Client(x.toMap())).get();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public void clientPoke(int clientId, String message)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("clientpoke", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("clid", Integer.toString(clientId)));
        command.add(new CommandSingleParameter("msg", message));

        executeCommand(command).complete();
    }

    /**
     * Kicks one or more clients specified with clid from their currently joined
     channel or from the server, depending on reasonid. The reasonmsg parameter
     specifies a text message sent to the kicked clients. This parameter is optional
     and may only have a maximum of 40 characters.
     */
    public void kick(Collection<Integer> clientIds, String message)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        MultiCommand command = new MultiCommand(
                "clientkick",
                ProtocolRole.CLIENT,
                clientIds.stream().map(x ->
                        new SingleCommand("clientkick", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("clid", Integer.toString(x))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("reasonid", Integer.toString(5)));
        command.add(new CommandSingleParameter("reasonmsg", message));

        executeCommand(command).complete();
    }
}
