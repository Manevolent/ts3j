package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.ComplexCommand;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

public class LocalClientHandlerConnected extends LocalClientHandlerFull {
    public LocalClientHandlerConnected(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    protected void handleCommand(ComplexCommand command) {

    }
}
