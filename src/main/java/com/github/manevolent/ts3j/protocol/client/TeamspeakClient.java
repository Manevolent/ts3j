package com.github.manevolent.ts3j.protocol.client;

import com.github.manevolent.ts3j.protocol.Endpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.function.Function;

public interface TeamspeakClient extends Endpoint {
    void connect(InetSocketAddress remote, String password, long timeout) throws IOException;

    SocketAddress getRemoteAddress();

    boolean isConnected();

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
}
