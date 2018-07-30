
package com.github.manevolent.ts3j.protocol.client;

import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.handler.client.*;
import com.github.manevolent.ts3j.protocol.socket.client.TeamspeakClientSocket;
import org.apache.commons.lang.reflect.ConstructorUtils;

public enum ClientConnectionState {
    CONNECTING(LocalClientHandlerConnecting.class),
    RETRIEVING_DATA(LocalClientHandlerRetrievingData.class),
    CONNECTED(LocalClientHandlerConnected.class),
    DISCONNECTED(LocalClientHandlerDisconnected.class);

    private final Class<? extends PacketHandler> clazz;

    ClientConnectionState(Class<? extends PacketHandler> clazz) {
        this.clazz = clazz;
    }

    public Class<? extends PacketHandler> getHandlerClass() {
        return clazz;
    }

    public PacketHandler createHandler(TeamspeakClientSocket client) {
        try {
            return (PacketHandler) ConstructorUtils.invokeConstructor(clazz, client);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}