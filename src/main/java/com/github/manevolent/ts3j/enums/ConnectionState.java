
package com.github.manevolent.ts3j.enums;

import com.github.manevolent.ts3j.TeamspeakClient;
import com.github.manevolent.ts3j.handler.*;

public enum ConnectionState {
    CONNECTING(TeamspeakClientHandlerConnecting.class),
    NEGOTIATING(TeamspeakClientHandlerNegotiating.class),
    CONNECTED(TeamspeakClientHandlerConnected.class),
    DISCONNECTED(TeamspeakClientHandlerDisconnected.class);

    private final Class<? extends TeamspeakClientHandler> clazz;

    ConnectionState(Class<? extends TeamspeakClientHandler> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends TeamspeakClientHandler> getHandlerClass() {
        return clazz;
    }

    public TeamspeakClientHandler createHandler(TeamspeakClient client) {
        try {
            return clazz.getConstructor(TeamspeakClient.class).newInstance(client);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}