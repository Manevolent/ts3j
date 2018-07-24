package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class Packet2Command extends Packet {
    public Packet2Command(ProtocolRole role) {
        super(PacketType.COMMAND, role);
    }

    @Override
    public void read(ByteBuffer buffer) {

    }

    @Override
    public void write(ByteBuffer buffer) {

    }

    @Override
    public int getSize() {
        return 0;
    }
}
