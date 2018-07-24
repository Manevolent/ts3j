package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;

import com.github.manevolent.ts3j.protocol.client.LocalTeamspeakClient;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;

import java.io.IOException;

public abstract class LocalClientHandler extends PacketHandler {
    private final LocalTeamspeakClient client;

    protected LocalClientHandler(LocalTeamspeakClient client) {
        super(client);

        this.client = client;
    }

    public void handleConnectionStateChanging(ClientConnectionState clientConnectionState) {

    }

    protected LocalTeamspeakClient getClient() {
        return client;
    }
}
