package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LocalClientHandlerConnected extends LocalClientHandlerFull {
    public LocalClientHandlerConnected(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException, TimeoutException {

    }
}
