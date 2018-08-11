package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class PacketBody7AckLow extends PacketBody {
    private int packetId;

    public PacketBody7AckLow(ProtocolRole role) {
        super(PacketBodyType.ACK_LOW, role);
    }

    public PacketBody7AckLow(ProtocolRole role, int packetId) {
        this(role);

        setPacketId(packetId);
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    @Override
    public void read(ByteBuffer buffer) {
        packetId = buffer.getShort() & 0xFFFF;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (getPacketId() & 0xFFFF));
    }

    @Override
    public int getSize() {
        return 2;
    }
}
