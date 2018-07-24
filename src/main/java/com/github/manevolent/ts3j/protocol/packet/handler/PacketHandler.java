package com.github.manevolent.ts3j.protocol.packet.handler;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.LocalEndpoint;

import java.io.IOException;

public abstract class PacketHandler {
    private final LocalEndpoint endpoint;

    protected PacketHandler(LocalEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void onUnassigning() throws IOException {

    }

    public void onAssigned() throws IOException {

    }

    public void handlePacket(NetworkPacket packet) throws IOException {

    }

    public LocalEndpoint getEndpoint() {
        return endpoint;
    }
}
