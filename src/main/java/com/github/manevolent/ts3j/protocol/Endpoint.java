package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.packet.Packet;

import java.io.IOException;
import java.util.function.Function;

public interface Endpoint {

    SocketRole getRole();

    void send(Packet packet) throws IOException;

}
