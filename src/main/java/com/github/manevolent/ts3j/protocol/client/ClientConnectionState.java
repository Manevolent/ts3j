
package com.github.manevolent.ts3j.protocol.client;

import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerConnected;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerConnecting;
import com.github.manevolent.ts3j.protocol.packet.handler.client.LocalClientHandlerDisconnected;

public enum ClientConnectionState {
    CONNECTING(LocalClientHandlerConnecting.class),
    CONNECTED(LocalClientHandlerConnected.class),
    DISCONNECTED(LocalClientHandlerDisconnected.class);

    private final Class<? extends LocalClientHandler> clazz;

    ClientConnectionState(Class<? extends LocalClientHandler> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends LocalClientHandler> getHandlerClass() {
        return clazz;
    }

    public LocalClientHandler createHandler(LocalTeamspeakClient client) {
        try {
            return clazz.getConstructor(LocalTeamspeakClient.class).newInstance(client);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}