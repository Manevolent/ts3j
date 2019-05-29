package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.api.Ban;
import com.github.manevolent.ts3j.api.Channel;
import com.github.manevolent.ts3j.api.Client;
import com.github.manevolent.ts3j.api.Permission;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.command.parameter.CommandOption;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.command.response.AbstractCommandResponse;
import com.github.manevolent.ts3j.command.response.CommandResponse;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.identity.Uid;
import com.github.manevolent.ts3j.protocol.*;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerConnected;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerConnecting;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerDisconnected;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerRetrievingData;
import com.github.manevolent.ts3j.protocol.packet.statistics.PacketStatistics;
import com.github.manevolent.ts3j.protocol.packet.transformation.InitPacketTransformation;
import com.github.manevolent.ts3j.util.HighPrecisionTimer;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import com.github.manevolent.ts3j.util.Pair;

import java.io.EOFException;
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

    private DatagramSocket socket;

    private final Map<String, CommandProcessor> namedProcessors = new HashMap<>();
    private ExecutorService commandExecutionService = Executors.newSingleThreadExecutor();
    private final Object commandSendLock = new Object();

    private volatile InetSocketAddress remote;

    private Microphone microphone;
    private Thread microphoneThread;

    private List<TS3Listener> listeners = new LinkedList<>();

    private Integer serverId = null;

    /**
     * The internal command buffer
     *
     * This buffer ascends through specific command codes, which are calculated using:
     *
     * get
     */
    private Map<Integer, ClientCommandResponse> awaitingCommands = new ConcurrentHashMap<>();

    public LocalTeamspeakClientSocket() {
        super(SocketRole.CLIENT);

        namedProcessors.put("initserver", new InitServerHandler());

        namedProcessors.put("channellist", new CommandProcessor() {
            @Override
            public void process(AbstractTeamspeakClientSocket client, SingleCommand singleCommand)
                    throws CommandProcessException {
                final ChannelListEvent channelListEvent = new ChannelListEvent(singleCommand.toMap());
                commandExecutionService.submit(() -> listeners.forEach(e -> e.onChannelList(channelListEvent)));
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

                try {
                    writePacket(new PacketBody2Command(getRole().getOut(), response));
                } catch (Exception e) {
                    Ts3Debugging.debug("Problem sending setconnectioninfo", e);
                }
            }
        });
    }

    public void setCommandExecutorService(ExecutorService executorService) {
        this.commandExecutionService = executorService;
    }

    /**
     * Enables or disables event callback multi-threading.  This is dangerous.  If this is enabled, events can be
     * processed out-of-order.  It is suggested you use your own threading system for particular events you feel safe
     * parallelling, such as chat messages.
     *
     * This is not enabled by default.
     *
     * @param enabled Event simultaneous (parallel) processing enabled
     */
    public void setEventMultiThreading(boolean enabled) {
        if (enabled)
            setCommandExecutorService(Executors.newCachedThreadPool());
        else
            setCommandExecutorService(Executors.newSingleThreadExecutor());
    }

    public void addListener(TS3Listener listener) {
        this.listeners.add(listener);
    }

    public boolean removeListener(TS3Listener listener) {
        return this.listeners.remove(listener);
    }

    @Override
    protected Class<? extends PacketHandler> getHandlerClass(ClientConnectionState state) {
        switch (state) {
            case DISCONNECTED:
                return LocalClientHandlerDisconnected.class;
            case CONNECTED:
                return LocalClientHandlerConnected.class;
            case RETRIEVING_DATA:
                return LocalClientHandlerRetrievingData.class;
            case CONNECTING:
                return LocalClientHandlerConnecting.class;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    protected NetworkPacket readNetworkPacket(int timeout) throws
            IOException,
            SocketTimeoutException {
        DatagramSocket socket = this.socket;
        InetSocketAddress remote = this.remote;

        final DatagramPacket packet = new DatagramPacket(new byte[500], 500);
        while (socket.isBound()) {
            socket.setSoTimeout(timeout);

            try {
                socket.receive(packet);
            } catch (SocketTimeoutException ex) {
                throw ex;
            } catch (SocketException ex) {
                if (socket.isClosed()) throw new EOFException();
                else throw ex;
            }

            if (remote == null || getState() == ClientConnectionState.DISCONNECTED)
                break;
            else if (!packet.getAddress().equals(remote.getAddress()) || packet.getPort() != remote.getPort())
                continue;

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

            Ts3Debugging.debug("[NETWORK] READ " + header.getType() +
                    " id=" + header.getPacketId() +
                    " len=" + packet.getLength() +
                    " from " + packet.getSocketAddress());

            return new NetworkPacket(
                    packet,
                    header,
                    buffer
            );
        }

        throw new IOException("disconnected");
    }

    @Override
    protected void writeNetworkPacket(NetworkPacket packet) throws IOException {
        InetSocketAddress remote = this.remote;
        ClientConnectionState state = this.getState();

        if (state == ClientConnectionState.DISCONNECTED)
            throw new IllegalStateException(state.name());
        else if (remote == null)
            throw new IOException(new NullPointerException("remote"));

        Ts3Debugging.debug("[NETWORK] WRITE " + packet.getHeader().getType() +
                " id=" + packet.getHeader().getPacketId() +
                " len=" + packet.getDatagram().getLength() +
                " to " + remote.toString());

        packet.getDatagram().setSocketAddress(remote);

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
    protected void onConnect() {
        super.onConnect();

        final ConnectedEvent connectedEvent = new ConnectedEvent(Collections.emptyMap());
        commandExecutionService.submit(() -> listeners.forEach(e -> e.onConnected(connectedEvent)));
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
        }

        serverId = null;

        remote = null;
    }

    /**
     * Initiates a connection to a server
     * @param hostname Hostname to contact
     * @param timeout timeout, in milliseconds, to complete a connection.
     */
    public void connect(String hostname, long timeout) throws IOException, TimeoutException {
        connect(hostname, null, timeout);
    }

    /**
     * Initiates a connection to a server
     * @param hostname Hostname to contact
     * @param password server password (may be null)
     * @param timeout timeout, in milliseconds, to complete a connection.
     */
    public void connect(String hostname, String password, long timeout) throws
            IOException,
            TimeoutException {

        InetSocketAddress address;

        try {
            // Attempt TS3DNS
            address = TS3DNS.lookup(hostname).stream().findFirst().orElseThrow(() -> new UnknownHostException(hostname));
        } catch (UnknownHostException e) {
            address = new InetSocketAddress(hostname, 9987);
        }

        connect(address, password, timeout);
    }

    /**
     * Initiates a connection to a server
     * @param remote remote sever to contact
     * @param password server password (may be null)
     * @param timeout timeout, in milliseconds, to complete a connection.
     */
    public void connect(InetSocketAddress remote, String password, long timeout)
            throws IOException, TimeoutException {
        try {
            ClientConnectionState connectionState = getState();

            if (connectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(connectionState.name());

            Ts3Debugging.debug("Connecting to " + remote + "...");

            // Set clid
            setClientId(0);

            // Resolve remote
            if (remote.isUnresolved())
                remote = new InetSocketAddress(remote.getAddress().getHostAddress(), remote.getPort());

            // Release old socket (if any)
            if (socket != null && !socket.isClosed())
                socket.close();

            // Construct new socket
            socket = new DatagramSocket();

            // Bind to remote
            this.remote = remote;

            // Free stuff
            serverId = null;


            synchronized (commandSendLock) {
                for (Integer id : new ArrayList<>(awaitingCommands.keySet())) {
                    ClientCommandResponse response = awaitingCommands.get(id);
                    response.completeFailure(new IOException("client reconnecting"));
                }

                awaitingCommands.clear();
            }

            setOption("client.hostname", remote.getHostString());

            if (password != null && password.length() > 0) setOption("client.password", password);

            setTransformation(new InitPacketTransformation());
            setState(ClientConnectionState.CONNECTING);

            waitForState(ClientConnectionState.CONNECTED, timeout);

            microphoneThread = new HighPrecisionTimer(
                    20,
                    5F, // up to 100ms of drift/catchup
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
        return super.isReading();
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
                awaitingCommandProcessor = awaitingCommands.get(getAwaitingCommandCode());
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

    /**
     * Executes a command.
     * @param command Command to execute.
     * @return Future, command response object.
     * @throws IOException if there is a connectivity-level exception.
     * @throws TimeoutException if command send times out.
     */
    public CommandResponse<Iterable<SingleCommand>> executeCommand(Command command)
            throws IOException, TimeoutException {
        return executeCommand(command, Function.identity());
    }

    private <T> CommandResponse<Iterable<T>> executeCommand(
            Command command,
            Function<SingleCommand, T> processor
    ) throws IOException, TimeoutException {
        final ClientCommandResponse<Iterable<T>> response;

        synchronized (commandSendLock) {
            if (getState() == ClientConnectionState.DISCONNECTED)
                throw new IOException("not connected");
            else if (getState() == ClientConnectionState.CONNECTING)
                throw new IOException("connecting");

            // Calculate the next command send code we should use
            // This is so we can sequentially send commands and expect responses in order from the server
            int sendCode = getNextCommandCode();
            if (sendCode >= Integer.MAX_VALUE) throw new ArrayIndexOutOfBoundsException("sendCode");

            command.add(new CommandSingleParameter("return_code", Integer.toString(sendCode)));

            awaitingCommands.put(sendCode, response = new ClientCommandResponse<>(sendCode, multiCommands -> {
                List<SingleCommand> commands = new LinkedList<>();
                for (MultiCommand multiCommand : multiCommands) commands.addAll(multiCommand.simplify());

                return commands.stream()
                        .map(processor::apply)
                        .collect(Collectors.toCollection(LinkedList::new));
            }, command));

            // Ensure that the next command we expect to receive a response for is sent to the network
            // but, only send that command if the accepting receive code matches the code we picked above
            // this ensures we don't double-send a command
            int nextCommandCode = getAwaitingCommandCode();
            if (nextCommandCode < 0) // This should never happen
                throw new IllegalStateException("nextCommandCode <= 0: " + nextCommandCode);
            else if (nextCommandCode == sendCode)
                sendCommand(nextCommandCode);
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

    /**
     * Gets the next command return_code we will create and buffer for sending.
     *
     * @return Next command send code
     */
    private int getNextCommandCode() {
        int highest = 0;
        for (Integer integer : awaitingCommands.keySet()) highest = Math.max(highest, integer);
        return highest + 1;
    }

    /**
     * Gets the next return_code we expect to receive from the server.
     *
     * @return command receive code
     */
    private int getAwaitingCommandCode() {
        if (awaitingCommands.size() <= 0) return -1;

        int lowest = Integer.MAX_VALUE;
        for (Integer integer : awaitingCommands.keySet()) lowest = Math.min(lowest, integer);
        return lowest;
    }

    private void sendCommand(int code)
            throws IOException, TimeoutException {
        synchronized (commandSendLock) {
            ClientCommandResponse currentResponse = awaitingCommands.get(code);
            if (currentResponse != null) currentResponse.ensureSent();
        }
    }

    private void sendNextCommand()
            throws IOException, TimeoutException {
        synchronized (commandSendLock) {
            ClientCommandResponse currentResponse = awaitingCommands.get(getAwaitingCommandCode());
            if (currentResponse != null) currentResponse.ensureSent();
        }
    }

    private class MicrophoneTask implements Runnable {
        private boolean wasSendingAudio = false;
        private byte sessionId = 1;
        private int flaggedPackets = 5;

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
                byte[] data = microphone.provide();
                if (data.length <= 0)
                    return;

                PacketBody0Voice voice = new PacketBody0Voice(getRole().getOut());

                voice.setCodecType(microphone.getCodec());
                voice.setCodecData(data);
                if (flaggedPackets > 0) voice.setServerFlag0(sessionId);

                try {
                    writePacket(voice);

                    flaggedPackets --;
                    wasSendingAudio = true;
                } catch (Exception e) {
                    // All we are really concerned about here is a disconnection event, in which case the loop
                    // will exit.
                    getExceptionHandler().accept(e);
                }
            } else if (wasSendingAudio) {
                PacketBody0Voice voice = new PacketBody0Voice(getRole().getOut());

                voice.setCodecType(microphone.getCodec());
                voice.setCodecData(new byte[0]);

                try {
                    writePacket(voice);

                    // reset decoder flag
                    flaggedPackets = 5;
                    sessionId ++;

                    if (sessionId >= 8)
                        sessionId = 1;

                    wasSendingAudio = false;
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
        private final Object sendLock = new Object();

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
                    handleComplete(multiCommand.simplifyOne());
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
                    handleComplete(command);
                } catch (IOException | TimeoutException e) {
                    throw new CommandProcessException(e);
                }
            else
                commands.add(new MultiCommand(command.getName(), ProtocolRole.CLIENT, command));
        }

        private void handleComplete(SingleCommand command) throws IOException, TimeoutException {
            try {
                int errorId = Integer.parseInt(command.get("id").getValue());

                switch (errorId) {
                    case 0:
                        completeSuccess(commands);
                        break;
                    default:
                        throw new CommandException(command.get("msg").getValue(), errorId);
                }
            } catch (Throwable e) {
                completeFailure(e);
            } finally {
                synchronized (commandSendLock) { // (this is likely already locked)
                    // Remove the current "awaiting" command.  This is removing this instance, effectively.
                    awaitingCommands.remove(getReturnCode());

                    // Ensure we send the next command
                    // This is essential to keep the command chain flowing when there are commands buffered in the
                    // command send (awaitingCommands) queue
                    sendNextCommand();
                }
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
                    LocalTeamspeakClientSocket.this.writePacket(new PacketBody2Command(ProtocolRole.CLIENT, command));
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
                Command command = new SingleCommand("clientupdate", ProtocolRole.CLIENT);
                command.add(new CommandSingleParameter("client_nickname", nickname));

                try {
                    executeCommand(command).complete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                break;
        }

        super.setNickname(nickname);
    }

    public void setHWID(String hwid) {
        setOption("client.hwid", hwid);
    }

    public String getHWID() {
        return getOption("client.hwid", String.class);
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

    public void setDescription(String description)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        setClientDescription(getClientId(), description);
    }

    public void setClientDescription(Client client, String description)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        setClientDescription(client.getId(), description);
    }

    public void setClientDescription(int clid, String description)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        editClient(clid, Collections.singletonMap("client_description", description));
    }

    public void editClient(Client client, Map<String, String> properties)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        editClient(client.getId(), properties);
    }

    public void editClient(int clid, Map<String, String> properties)
            throws InterruptedException, ExecutionException, TimeoutException, CommandException, IOException {
        MultiCommand command = new MultiCommand(
                "clientedit",
                ProtocolRole.CLIENT,
                properties.entrySet().stream().map(x ->
                        new SingleCommand("clientedit", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter(x.getKey(), x.getValue())
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("clid", Integer.toString(clid)));

        executeCommand(command).complete();
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

    /*
    public void clientListPermission(int clientDatabaseId)
            throws IOException, TimeoutException, ExecutionException, InterruptedException {
        MultiCommand command = new MultiCommand(
                "clientpermlist",
                ProtocolRole.CLIENT,
                new SingleCommand("clientlistperm", ProtocolRole.CLIENT)
        );

        command.add(new CommandSingleParameter("cldbid", Integer.toString(clientDatabaseId)));
        command.add(new CommandOption("permsid"));

        executeCommand(command).complete();
    }
    */

    public void clientAddPermission(int clientDatabaseId, Permission... permission)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        MultiCommand command = new MultiCommand(
                "clientaddperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("clientaddperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue())),
                                new CommandSingleParameter("permskip", Integer.toString(x.isNegated() ? 1 : 0))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cldbid", Integer.toString(clientDatabaseId)));

        executeCommand(command).complete();
    }

    public void clientDeletePermission(int clientDatabaseId, Permission... permission)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        MultiCommand command = new MultiCommand(
                "clientdelperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("clientdelperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue())),
                                new CommandSingleParameter("permskip", Integer.toString(x.isNegated() ? 1 : 0))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cldbid", Integer.toString(clientDatabaseId)));

        executeCommand(command).complete();
    }

    public void channelAddPermission(int channelId, Permission... permission)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        MultiCommand command = new MultiCommand(
                "channeladdperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channeladdperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue())),
                                new CommandSingleParameter("permskip", Integer.toString(x.isNegated() ? 1 : 0))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));

        executeCommand(command).complete();
    }

    public void channelClientAddPermission(int channelId, int clientDatabaseId, Permission... permission)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        MultiCommand command = new MultiCommand(
                "channelclientaddperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channelclientaddperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue())),
                                new CommandSingleParameter("permskip", Integer.toString(x.isNegated() ? 1 : 0))
                        )).collect(Collectors.toList()
                ));

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("cldbid", Integer.toString(channelId)));

        executeCommand(command).complete();
    }

    public void channelClientDeletePermission(int channelId, int clientDatabaseId, Permission... permission)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        MultiCommand command = new MultiCommand(
                "channeldelperm",
                ProtocolRole.CLIENT,
                Arrays.stream(permission).map(x ->
                        new SingleCommand("channeldelperm", ProtocolRole.CLIENT, // name is redundant here
                                new CommandSingleParameter("permsid", x.getName()),
                                new CommandSingleParameter("permvalue", Integer.toString(x.getValue())),
                                new CommandSingleParameter("permnegated", Integer.toString(x.isNegated() ? 1 : 0))
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("clientmove", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("cid", Integer.toString(channelId)));
        if (password != null) command.add(new CommandSingleParameter("cpw", Integer.toString(channelId)));
        command.add(new CommandSingleParameter("clid", Integer.toString(clientId)));

        executeCommand(command).complete();
    }

    public void joinChannel(int channelId, String password)
            throws InterruptedException, TimeoutException, IOException, CommandException {
        clientMove(getClientId(), channelId, password);
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public Iterable<Channel> listChannels()
            throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("channellist", ProtocolRole.CLIENT);

        return executeCommand(command, x -> new Channel(x.toMap())).get();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public Iterable<Client> listClients()
            throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("clientlist", ProtocolRole.CLIENT);

        return executeCommand(command, x -> new Client(x.toMap())).get();
    }

    public void serverGroupAddClient(int groupId, int clientDatabaseId)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("servergroupaddclient", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("sgid", Integer.toString(groupId)));
        command.add(new CommandSingleParameter("cldbid", Integer.toString(clientDatabaseId)));

        executeCommand(command).complete();
    }

    public void serverGroupRemoveClient(int groupId, int clientDatabaseId)
            throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("servergroupdelclient", ProtocolRole.CLIENT);

        command.add(new CommandSingleParameter("sgid", Integer.toString(groupId)));
        command.add(new CommandSingleParameter("cldbid", Integer.toString(clientDatabaseId)));

        executeCommand(command).complete();
    }

    /**
     * Moves one or more clients specified with clid to the channel with ID cid. If
     the target channel has a password, it needs to be specified with cpw. If the
     channel has no password, the parameter can be omitted.
     */
    public void clientPoke(int clientId, String message)
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
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

    public void subscribeAll() throws IOException, TimeoutException, InterruptedException, CommandException {
        Command command = new SingleCommand("channelsubscribeall", ProtocolRole.CLIENT);
        executeCommand(command).get();
    }

    public Client getClientInfo(int clientId)
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
            throws IOException, TimeoutException, InterruptedException, CommandException {
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
        disconnect(null);
    }

    public void disconnect(String reason)
            throws
            IOException, TimeoutException,
            ExecutionException, InterruptedException {
        try {
            switch (getState()) {
                case CONNECTING:
                case RETRIEVING_DATA:
                    waitForState(ClientConnectionState.CONNECTED, 10000L);
                case CONNECTED:
                    if (reason == null) {
                        writePacket(new PacketBody2Command(
                                ProtocolRole.CLIENT,
                                new SingleCommand("clientdisconnect", ProtocolRole.CLIENT))
                        );
                    } else {
                        writePacket(new PacketBody2Command(
                                ProtocolRole.CLIENT,
                                new SingleCommand(
                                        "clientdisconnect", ProtocolRole.CLIENT,
                                        new CommandSingleParameter("reasonid", "8"),
                                        new CommandSingleParameter("reasonmsg", reason)
                                ))
                        );
                    }

                    waitForState(ClientConnectionState.DISCONNECTED, 30000L);

                    break;
            }
        } finally {
            setState(ClientConnectionState.DISCONNECTED);
        }
    }

}
