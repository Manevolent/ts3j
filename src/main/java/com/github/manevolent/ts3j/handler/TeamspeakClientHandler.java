package com.github.manevolent.ts3j.handler;

import com.github.manevolent.ts3j.TeamspeakClient;
import com.github.manevolent.ts3j.enums.ConnectionState;

import java.io.IOException;
import java.net.DatagramPacket;

public abstract class TeamspeakClientHandler {
    private final TeamspeakClient client;

    protected TeamspeakClientHandler(TeamspeakClient client) {
        this.client = client;
    }

    public void handleConnectionStateChanging(ConnectionState connectionState) {

    }

    public void handleNetworkPacket(DatagramPacket packet) {

    }

    public void onAssigned() throws IOException {

    }

    protected TeamspeakClient getClient() {
        return client;
    }
}
