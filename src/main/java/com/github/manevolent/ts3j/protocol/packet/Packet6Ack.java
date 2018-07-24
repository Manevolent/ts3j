package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Packet6Ack extends Packet {
    private int packetId;

    public Packet6Ack(ProtocolRole role) {
        super(PacketType.ACK, role);
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
