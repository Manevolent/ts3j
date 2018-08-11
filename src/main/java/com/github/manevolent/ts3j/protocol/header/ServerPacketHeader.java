package com.github.manevolent.ts3j.protocol.header;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;

import java.nio.ByteBuffer;

public class ServerPacketHeader extends PacketHeader {
    private int packetFlags;

    public ServerPacketHeader() {
        super(ProtocolRole.SERVER);
    }

    public int getPacketFlags() {
        return packetFlags;
    }

    public void setPacketFlags(int packetFlags) {
        this.packetFlags = packetFlags;
    }

    @Override
    protected ByteBuffer writeHeader(ByteBuffer buffer) {
        super.writeHeader(buffer);

        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md

        buffer.put((byte) ((getType().getIndex() & 0x0F) | (getPacketFlags() & 0xF0))); // PT / Packet Type + Flags

        // (payload) -- see writeBody impl in higher levels

        return buffer;
    }

    @Override
    protected ByteBuffer readHeader(ByteBuffer buffer) {
        super.readHeader(buffer);

        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md
        byte packetTypeAndFlags = buffer.get();
        setType(PacketBodyType.fromId((packetTypeAndFlags & 0x0F))); // PT / Packet Type + Flags
        setPacketFlags(packetTypeAndFlags & 0xF0);
        // (payload) -- see writeBody impl in higher levels

        return buffer;
    }

    @Override
    public int getSize() {
        return super.getSize() + 1;
    }

    @Override
    public PacketHeader clone() {
        ServerPacketHeader header = new ServerPacketHeader();
        header.setPacketId(getPacketId());
        header.setType(getType());
        header.setGeneration(getGeneration());
        header.setPacketFlags(getPacketFlags());

        byte[] mac = new byte[8];
        System.arraycopy(getMac(), 0, mac, 0, 8);
        header.setMac(mac);

        return header;
    }
}