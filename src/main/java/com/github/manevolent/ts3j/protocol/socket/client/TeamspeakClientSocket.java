package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.packet.PacketBody;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.TeamspeakSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface TeamspeakClientSocket extends TeamspeakSocket {

    Map<String, Object> getOptions();

    default <T extends Object> T getOption(String key, Class<T> clazz) {
        Object value = getOptions().get(key);

        if (value == null) return (T) null;

        else if (!value.getClass().isAssignableFrom(clazz)) {
            throw new ClassCastException(
                    clazz.getName()
                            + " is not assignable to option type " +
                            value.getClass().getName()
            );
        } else
            return (T) value;
    }

    default Object setOption(String key, Object value) {
        return getOptions().put(key, value);
    }

    boolean isConnected();

    Identity getIdentity();

    void setIdentity(Identity identity);

    PacketTransformation getTransformation();

    void setTransformation(PacketTransformation transformation);

    PacketHandler getHandler();

    Packet readPacket() throws IOException, TimeoutException;

    void writePacket(PacketBody body) throws IOException, TimeoutException;

    void writePacket(Packet packet) throws IOException, TimeoutException;

}
