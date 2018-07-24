package com.github.manevolent.ts3j.protocol.socket;

import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;

import java.io.IOException;

public interface TeamspeakSocket {
    SocketRole getSocketRole();

    void send(PacketHeader header, Packet packet) throws IOException;

    PacketTransformation getPacketTransformation();

    void setPacketTransformation(PacketTransformation transformation);

}
