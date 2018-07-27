package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

public interface CommandProcessor {
    void process(AbstractTeamspeakClientSocket socket, ComplexCommand complexCommand)
            throws CommandProcessException;
}
