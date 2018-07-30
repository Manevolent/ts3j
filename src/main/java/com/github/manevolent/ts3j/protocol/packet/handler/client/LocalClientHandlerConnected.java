package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

public class LocalClientHandlerConnected extends LocalClientHandlerFull {
    public LocalClientHandlerConnected(LocalTeamspeakClientSocket client) {
        super(client);
    }
}
