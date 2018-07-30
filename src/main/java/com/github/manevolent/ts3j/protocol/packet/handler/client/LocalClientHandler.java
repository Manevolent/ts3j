package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;

import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

public abstract class LocalClientHandler extends PacketHandler {
    private final LocalTeamspeakClientSocket client;

    protected LocalClientHandler(LocalTeamspeakClientSocket client) {
        super(client);

        this.client = client;
    }

    public void handleConnectionStateChanging(ClientConnectionState clientConnectionState) {

    }

    public LocalTeamspeakClientSocket getClient() {
        return client;
    }
}
