package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PacketBody5Pong extends PacketBody {
    private int packetId;

    public PacketBody5Pong(ProtocolRole role) {
        super(PacketBodyType.PONG, role);
    }

    public PacketBody5Pong(ProtocolRole role, int packetId) {
        this(role);

        setPacketId(packetId);
    }

    @Override
    public void setHeaderValues(PacketHeader header) {
        header.setPacketFlag(HeaderFlag.UNENCRYPTED, true);
    }

    @Override
    public void read(ByteBuffer buffer) {
        packetId = buffer.getShort() & 0x000000FFFFF;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (packetId));
    }

    @Override
    public int getSize() {
        return 2;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        if (packetId > 65535 || packetId < 0)
            throw new IllegalArgumentException("packetId out of bounds: " + packetId);

        this.packetId = packetId;
    }
}
