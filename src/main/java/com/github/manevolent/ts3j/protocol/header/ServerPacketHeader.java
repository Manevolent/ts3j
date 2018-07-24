package com.github.manevolent.ts3j.protocol.header;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.packet.PacketType;
import com.github.manevolent.ts3j.util.Ts3Logging;

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
        setType(PacketType.fromId((packetTypeAndFlags & 0x0F))); // PT / Packet Type + Flags
        setPacketFlags(packetTypeAndFlags & 0xF0);
        // (payload) -- see writeBody impl in higher levels

        return buffer;
    }

    @Override
    public int getSize() {
        return super.getSize() + 1;
    }
}