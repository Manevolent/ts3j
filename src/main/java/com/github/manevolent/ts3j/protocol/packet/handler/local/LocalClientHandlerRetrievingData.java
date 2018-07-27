package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.ComplexCommand;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LocalClientHandlerRetrievingData extends LocalClientHandlerFull {
    public LocalClientHandlerRetrievingData(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    protected void handleCommand(ComplexCommand command) {

    }
}
