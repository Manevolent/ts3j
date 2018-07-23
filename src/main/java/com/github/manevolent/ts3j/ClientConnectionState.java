
package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.handler.*;

public enum ClientConnectionState {
    CONNECTING(TeamspeakClientHandlerConnecting.class),
    CONNECTED(TeamspeakClientHandlerConnected.class),
    DISCONNECTED(TeamspeakClientHandlerDisconnected.class);

    private final Class<? extends TeamspeakClientHandler> clazz;

    ClientConnectionState(Class<? extends TeamspeakClientHandler> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends TeamspeakClientHandler> getHandlerClass() {
        return clazz;
    }

    public TeamspeakClientHandler createHandler(Teamspeak3Client client) {
        try {
            return clazz.getConstructor(Teamspeak3Client.class).newInstance(client);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}