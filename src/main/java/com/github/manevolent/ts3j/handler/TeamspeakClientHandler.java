package com.github.manevolent.ts3j.handler;

import com.github.manevolent.ts3j.Teamspeak3Client;
import com.github.manevolent.ts3j.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.NetworkPacket;

import java.io.IOException;

public abstract class TeamspeakClientHandler {
    private final Teamspeak3Client client;

    protected TeamspeakClientHandler(Teamspeak3Client client) {
        this.client = client;
    }

    protected final Teamspeak3Client getClient() {
        return client;
    }

    public void handleConnectionStateChanging(ClientConnectionState clientConnectionState) {

    }

    public void onAssigned() throws IOException {

    }

    public void handlePacket(NetworkPacket packet) throws IOException {

    }
}
