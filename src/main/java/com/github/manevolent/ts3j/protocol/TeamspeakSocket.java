package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.Packet;

import java.io.IOException;

public interface TeamspeakSocket {
    SocketRole getSocketRole();

    void send(PacketHeader header, Packet packet) throws IOException;
}
