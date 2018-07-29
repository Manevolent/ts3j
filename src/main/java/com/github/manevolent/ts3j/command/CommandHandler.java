package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.client.ClientChannelListFinishedProcessor;
import com.github.manevolent.ts3j.command.client.ClientChannelListProcessor;
import com.github.manevolent.ts3j.command.client.ClientInitServerProcessor;
import com.github.manevolent.ts3j.command.client.ClientNotifyClientEnterViewProcessor;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.util.HashMap;
import java.util.Map;

public final class CommandHandler implements CommandProcessor {
    private final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    protected CommandHandler() {

    }

    @Override
    public void process(AbstractTeamspeakClientSocket socket, MultiCommand multiCommand)
            throws CommandProcessException {
        CommandProcessor processor = commandProcessors.get(multiCommand.getName().toLowerCase());
        if (processor != null)
            processor.process(socket, multiCommand);
    }

    @Override
    public void process(AbstractTeamspeakClientSocket socket, SingleCommand command)
            throws CommandProcessException {
        CommandProcessor processor = commandProcessors.get(command.getName().toLowerCase());
        if (processor != null)
            processor.process(socket, command);
    }

    public static CommandHandler createLocalClientHandler() {
        CommandHandler handler = new CommandHandler();

        handler.commandProcessors.put("initserver", new ClientInitServerProcessor());
        handler.commandProcessors.put("channellist", new ClientChannelListProcessor());
        handler.commandProcessors.put("notifycliententerview", new ClientNotifyClientEnterViewProcessor());
        handler.commandProcessors.put("channellistfinished", new ClientChannelListFinishedProcessor());

        return handler;
    }
}
