package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.client.ClientInitServerProcessor;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.util.HashMap;
import java.util.Map;

public final class CommandHandler implements CommandProcessor {
    private final Map<String, CommandProcessor> commandProcessors = new HashMap<>();

    protected CommandHandler() {

    }

    @Override
    public void process(AbstractTeamspeakClientSocket socket, ComplexCommand complexCommand)
            throws CommandProcessException {
        Ts3Debugging.debug("[COMMAND] " + complexCommand.build());

        CommandProcessor processor = commandProcessors.get(complexCommand.getName().toLowerCase());
        if (processor != null)
            processor.process(socket, complexCommand);
    }

    public static CommandHandler createLocalClient() {
        CommandHandler handler = new CommandHandler();

        handler.commandProcessors.put("initserver", new ClientInitServerProcessor());

        return handler;
    }
}
