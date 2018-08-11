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
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.identity.Uid;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.PacketKind;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.statistics.PacketStatistics;
import com.github.manevolent.ts3j.util.HighPrecisionRecurrentTask;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import com.github.manevolent.ts3j.util.Pair;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
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
    private final ExecutorService commandExecutionService = Executors.newCachedThreadPool();
    private final Object commandSendLock = new Object();


    private Microphone microphone;
    private Thread microphoneThread;

    private List<TS3Listener> listeners = new LinkedList<>();

    private int acceptingReturnCode = Integer.MAX_VALUE;
    private int lastReturnCode = 0;

    private Integer serverId = null;

    private Map<Integer, ClientCommandResponse> awaitingCommands = new ConcurrentHashMap<>();

    public LocalTeamspeakClientSocket() {
        super();

        namedProcessors.put("initserver", new InitServerHandler());

        namedProcessors.put("channellist", new CommandProcessor() {
            @Override
            public void process(AbstractTeamspeakClientSocket client, SingleCommand singleCommand)
                    throws CommandProcessException {
                ChannelListEvent channelListEvent = new ChannelListEvent(singleCommand.toMap());
                for (TS3Listener listener : listeners) listener.onChannelList(channelListEvent);
            }
        });

        namedProcessors.put("channellistfinished", new ChannelListFinishedHandler());

        namedProcessors.put("notifyclientleftview", new CommandProcessor() {
            @Override
            public void process(AbstractTeamspeakClientSocket client, SingleCommand command)
                    throws CommandProcessException {
                int clientId = Integer.parseInt(command.get("clid").getValue());
                if (clientId == getClientId()) {
                    try {
                        setState(ClientConnectionState.DISCONNECTED);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }


                    DisconnectedEvent e = new DisconnectedEvent(command.toMap());
                    commandExecutionService.submit(() -> listeners.forEach(e::fire));
                } else {
                    ClientLeaveEvent e = new ClientLeaveEvent(command.toMap());
                    commandExecutionService.submit(() -> listeners.forEach(e::fire));
                }
            }
        });

        namedProcessors.put("notifyconnectioninforequest", new CommandProcessor() {
            @Override
            public void process(AbstractTeamspeakClientSocket client, SingleCommand singleCommand)
                    throws CommandProcessException {
                SingleCommand response = new SingleCommand("setconnectioninfo", getRole().getOut());

                Pair<Double, Double> ping = getPing();
                response.add(new CommandSingleParameter("connection_ping",
                        Double.toString(Math.round(ping.getKey() * 1000D))));
                response.add(new CommandSingleParameter("connection_ping_deviation",
                        Double.toString(Math.round(ping.getValue() * 1000D * 1000D) / 1000D)));

                for (PacketKind kind : PacketKind.values()) {
                    PacketStatistics stats = getStatistics(kind);
                    String name = kind.name().toLowerCase();
                    response.add(new CommandSingleParameter("connection_packets_sent_" + name,
                            Integer.toString(stats.getSentPackets())));
                    response.add(new CommandSingleParameter("connection_packets_received_" + name,
                            Integer.toString(stats.getReceivedPackets())));
                    response.add(new CommandSingleParameter("connection_bytes_sent_" + name,
                            Integer.toString(stats.getSentBytes())));
                    response.add(new CommandSingleParameter("connection_bytes_received_" + name,
                            Integer.toString(stats.getReceivedBytes())));
                    response.add(new CommandSingleParameter("connection_server2client_packetloss_" + name,
                            Integer.toString(0)));
                    response.add(new CommandSingleParameter("connection_bandwidth_sent_last_second_" + name,
                            Integer.toString(stats.getSentBytesLastSecond())));
                    response.add(new CommandSingleParameter("connection_bandwidth_sent_last_minute_" + name,
                            Integer.toString(stats.getSentBytesLastMinute())));
                    response.add(new CommandSingleParameter("connection_bandwidth_received_last_second_" + name,
                            Integer.toString(stats.getReceivedBytesLastSecond())));
                    response.add(new CommandSingleParameter("connection_bandwidth_received_last_minute_" + name,
                            Integer.toString(stats.getReceivedBytesLastMinute())));
                }

                response.add(new CommandSingleParameter("connection_server2client_packetloss_total", Integer.toString(0)));

                Ts3Debugging.debug(response.build());

                try {
                    writePacket(new PacketBody2Command(getRole().getOut(), response));
                } catch (Exception e) {
                    Ts3Debugging.debug("Problem sending setconnectioninfo", e);
                }
            }
        });
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

    @Override
    protected void onDisconnect() {
        super.onDisconnect();

        synchronized (commandSendLock) {
            for (Integer id : new ArrayList<>(awaitingCommands.keySet())) {
                ClientCommandResponse response = awaitingCommands.get(id);
                response.completeFailure(new IOException("client disconnected"));
            }

            awaitingCommands.clear();

            calculateAcceptingReturnCode();
        }

        serverId = null;
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

            serverId = null;
            awaitingCommands.clear();
            acceptingReturnCode = lastReturnCode = 0;

            setOption("client.hostname", remote.getHostString());

            if (password != null) setOption("client.password", password);

            socket.connect(remote);

            setState(ClientConnectionState.CONNECTING);

            waitForState(ClientConnectionState.CONNECTED, timeout);

            microphoneThread = new HighPrecisionRecurrentTask(
                    20,
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

            commandExecutionService.submit(() -> listeners.forEach(event::fire));
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
        return executeCommand(
                new SingleCommand("channellist", ProtocolRole.CLIENT),
                command -> new Channel(command.toMap())
        );
    }

    private CommandResponse<Iterable<SingleCommand>> executeCommand(Command command)
            throws IOException, TimeoutException {
        return executeCommand(command, Function.identity());
    }

    private <T> CommandResponse<Iterable<T>> executeCommand(
            Command command,
            Function<SingleCommand, T> processor
    ) throws IOException, TimeoutException {
        final ClientCommandResponse<Iterable<T>> response;

        synchronized (commandSendLock) {
            if (getState() != ClientConnectionState.CONNECTED)
                throw new IOException("not connected");

            int returnCode;

            // sync may not be necessary here
            returnCode = lastReturnCode++;

            command.add(new CommandSingleParameter("return_code", Integer.toString(returnCode)));

            awaitingCommands.put(returnCode, response = new ClientCommandResponse<>(returnCode, multiCommands -> {
                List<SingleCommand> commands = new LinkedList<>();
                for (MultiCommand multiCommand : multiCommands) commands.addAll(multiCommand.simplify());

                return commands.stream()
                        .map(processor::apply)
                        .collect(Collectors.toCollection(LinkedList::new));
            }, command));

            acceptingReturnCode = calculateAcceptingReturnCode();

            if (acceptingReturnCode == returnCode)
                sendNextCommand();
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

    private void sendNextCommand()
            throws IOException, TimeoutException {
        synchronized (commandSendLock) {
            ClientCommandResponse currentResponse = awaitingCommands.get(acceptingReturnCode);
            if (currentResponse != null) currentResponse.ensureSent();
        }
    }

    private class MicrophoneTask implements Runnable {
        @Override
        public void run() {
            if (!isConnected()) return;

            Microphone microphone;

            // Attempt to get the current microphone instance, saving it in a variable to prevent
            // any race conditions by accessing the parent class' this.microphone.
            if ((microphone = LocalTeamspeakClientSocket.this.microphone) == null)
                return;

            if (microphone.isMuted()) {
                // TODO
                // Set a state and send a muted event
            } else if (microphone.isReady()) {
                PacketBody0Voice voice = new PacketBody0Voice(getRole().getOut());

                voice.setCodecType(microphone.getCodec());
                voice.setCodecData(microphone.provide());

                try {
                    writePacket(voice);
                } catch (Exception e) {
                    // All we are really concerned about here is a disconnection event, in which case the loop
                    // will exit.
                    getExceptionHandler().accept(e);
                }
            }
        }
    }

    private class ClientCommandResponse<T>
            extends AbstractCommandResponse<T>
            implements CommandProcessor {
        private final int returnCode;
        private final Command command;
        private final List<MultiCommand> commands = new LinkedList<>();

        private Object sendLock = new Object();
        private boolean sent = false;

        public ClientCommandResponse(int returnCode,
                                     Function<Iterable<MultiCommand>, T> processor,
                                     Command command) {
            super(processor);
            this.returnCode = returnCode;
            this.command = command;
        }

        @Override
        public void process(AbstractTeamspeakClientSocket client,
                             MultiCommand multiCommand)
                throws CommandProcessException {
            if (multiCommand.getName().equalsIgnoreCase("error"))
                try {
                    handleError(multiCommand.simplifyOne());
                } catch (IOException | TimeoutException e) {
                    throw new CommandProcessException(e);
                }
            else
                commands.add(multiCommand);
        }

        @Override
        public void process(AbstractTeamspeakClientSocket client,
                            SingleCommand command)
                throws CommandProcessException {
            if (command.getName().equalsIgnoreCase("error"))
                try {
                    handleError(command);
                } catch (IOException | TimeoutException e) {
                    throw new CommandProcessException(e);
                }
            else
                commands.add(new MultiCommand(command.getName(), ProtocolRole.CLIENT, command));
        }

        private void handleError(SingleCommand command) throws IOException, TimeoutException {
            try {
                int errorId = Integer.parseInt(command.get("id").getValue());

                synchronized (commandSendLock) {
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
            } finally {
                sendNextCommand();
            }
        }

        public int getReturnCode() {
            return returnCode;
        }

        @Override
        public Command getCommand() {
            return command;
        }

        public boolean ensureSent()
                throws IOException, TimeoutException {
            synchronized (sendLock) {
                if (!sent) {
                    LocalTeamspeakClientSocket.this.writePacket(
                            new PacketBody2Command(ProtocolRole.CLIENT, command)
                    );

                    sent = true;

                    return true;
                }

                return false;
            }
        }
    }

    private class ChannelListFinishedHandler implements CommandProcessor {
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

                // Must use super because we're probably in RETRIEVING_DATA state and we'd block on this.
                LocalTeamspeakClientSocket.super.setNickname(singleCommand.get("acn").getValue());

                serverId = Integer.parseInt(singleCommand.get("virtualserver_id").getValue());
            } catch (Exception e) {
                throw new CommandProcessException(e);
            }
        }
    }

    @Override
    public void setNickname(String nickname) {
        switch (getState()) {
            case RETRIEVING_DATA:
            case CONNECTED:
                Command banCommand = new SingleCommand("clientupdate", ProtocolRole.CLIENT);

                banCommand.add(new CommandSingleParameter("client_nickname", nickname));

                try {
                    executeCommand(banCommand).complete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                break;
        }

        super.setNickname(nickname);
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

        return executeCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public Iterable<Ban> banDatabaseClient(int dbid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banclient", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("cldbid", Integer.toString(dbid)));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        return executeCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public Iterable<Ban> banClient(Uid uid, Integer timeInSeconds, String reason)
            throws IOException, TimeoutException, ExecutionException, InterruptedException, CommandException {
        Command banCommand = new SingleCommand("banclient", ProtocolRole.CLIENT);

        banCommand.add(new CommandSingleParameter("uid", uid.toBase64()));
        if (timeInSeconds != null) banCommand.add(new CommandSingleParameter("time", timeInSeconds.toString()));
        if (reason != null) banCommand.add(new CommandSingleParameter("banreason", reason));

        return executeCommand(banCommand, command -> new Ban(command.toMap())).get();
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
        return executeCommand(banCommand, command -> new Ban(command.toMap())).get();
    }

    public void deleteBan(Ban ban)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        deleteBan(ban.getId());
    }

    public void channelAddPermission(int channelId, Permission... permission)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
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

        executeCommand(command).complete();
    }

    public void channelClientAddPermission(int channelId, int clientDatabaseId, Permission... permission)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
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

        executeCommand(command).complete();
    }

    public void channelClientDeletePermission(int channelId, int clientDatabaseId, Permission... permission)
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

        return executeCommand(command, x -> new Client(x.toMap())).get();
    }

    /**
     * Sends a text message a specified target. The type of the target is determined by targetmode while target
     specifies the ID of the recipient, whether it be a virtual server, a channel or a client.
     */
    public void sendServerMessage(String message)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("sendtextmessage", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("targetmode", Integer.toString(3)));
        command.add(new CommandSingleParameter("target", Integer.toString(serverId)));
        command.add(new CommandSingleParameter("msg", message));

        executeCommand(command).complete();
    }

    /**
     * Sends a text message a specified target. The type of the target is determined by targetmode while target
     specifies the ID of the recipient, whether it be a virtual server, a channel or a client.
     */
    public void sendChannelMessage(int channelId, String message)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("sendtextmessage", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("targetmode", Integer.toString(2)));
        command.add(new CommandSingleParameter("target", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("msg", message));

        executeCommand(command).complete();
    }

    /**
     * Sends a text message a specified target. The type of the target is determined by targetmode while target
     specifies the ID of the recipient, whether it be a virtual server, a channel or a client.
     */
    public void sendPrivateMessage(int clientId, String message)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("sendtextmessage", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("targetmode", Integer.toString(1)));
        command.add(new CommandSingleParameter("target", Integer.toString(clientId)));
        command.add(new CommandSingleParameter("msg", message));

        executeCommand(command).complete();
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

        return executeCommand(command, x -> new Channel(x.toMap())).get();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public Iterable<Client> listClients()
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("clientlist", ProtocolRole.CLIENT);

        return executeCommand(command, x -> new Client(x.toMap())).get();
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

    public void subscribeAll() throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand("channelsubscribeall", ProtocolRole.CLIENT);
        executeCommand(command).get();
    }

    public Client getClientInfo(int clientId)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand(
                "clientinfo",
                ProtocolRole.CLIENT,
                new CommandSingleParameter("clid", Integer.toString(clientId))
        );

        Iterable<Client> clients = executeCommand(command, x -> {
            Map<String, String> map = x.toMap();
            map.put("clid", Integer.toString(clientId));
            return new Client(map);
        }).get();

        Iterator<Client> iterator = clients.iterator();
        if (iterator.hasNext())
            return iterator.next();
        else
            return null;
    }

    public Channel getChannelInfo(int channelId)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        Command command = new SingleCommand(
                "channelinfo",
                ProtocolRole.CLIENT,
                new CommandSingleParameter("cid", Integer.toString(channelId))
        );

        Iterator<Channel> iterator = executeCommand(command, x -> new Channel(x.toMap())).get().iterator();
        if (iterator.hasNext())
            return iterator.next();
        else
            return null;
    }

    public void disconnect()
            throws
            IOException, TimeoutException,
            ExecutionException, InterruptedException {
        switch (getState()) {
            case CONNECTING:
            case RETRIEVING_DATA:
                waitForState(ClientConnectionState.CONNECTED, 10000L);
            case CONNECTED:
                writePacket(new PacketBody2Command(
                        ProtocolRole.CLIENT,
                        new SingleCommand("clientdisconnect", ProtocolRole.CLIENT))
                );

                break;
        }
    }

}
