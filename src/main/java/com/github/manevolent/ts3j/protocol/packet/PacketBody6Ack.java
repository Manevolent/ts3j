package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class PacketBody6Ack extends PacketBody {
    private int packetId;

    public PacketBody6Ack(ProtocolRole role) {
        super(PacketBodyType.ACK, role);
    }

    public PacketBody6Ack(ProtocolRole role, int packetId) {
        super(PacketBodyType.ACK, role);

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
        packetId = buffer.getShort() & 0x0000FFFF;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (getPacketId() & 0xFFFFFF));
    }

    @Override
    public int getSize() {
        return 2;
    }
}
