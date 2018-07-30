package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

public interface CommandProcessor {
    default void process(AbstractTeamspeakClientSocket client,
                 MultiCommand multiCommand)
            throws CommandProcessException {
        for (SingleCommand singleCommand : multiCommand.simplify()) process(client, singleCommand);
    }

    default void process(AbstractTeamspeakClientSocket client,
                 SingleCommand singleCommand)
            throws CommandProcessException { }
}
