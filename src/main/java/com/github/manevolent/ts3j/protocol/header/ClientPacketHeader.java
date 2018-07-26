package com.github.manevolent.ts3j.protocol.header;

import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class ClientPacketHeader extends PacketHeader {
    private int clientId;

    public ClientPacketHeader() {
        super(ProtocolRole.CLIENT);
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    @Override
    protected ByteBuffer writeHeader(ByteBuffer buffer) {
        super.writeHeader(buffer);

        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md

        buffer.putShort((short) (getClientId() & 0x0000FFFF)); // CId / Client Id
        buffer.put((byte) ((getType().getIndex() & 0x0F) | (getPacketFlags() & 0xF0))); // PT / Packet Type + Flags

        // (payload) -- see writeBody impl in higher levels

        return buffer;
    }


    @Override
    protected ByteBuffer readHeader(ByteBuffer buffer) {
        super.readHeader(buffer);

        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md

        setClientId((buffer.getShort() & 0xFFFF));

        byte packetTypeAndFlags = buffer.get();
        setType(PacketBodyType.fromId((packetTypeAndFlags & 0x0F))); // PT / Packet Type + Flags
        setPacketFlags(packetTypeAndFlags & 0xF0);

        // (payload) -- see writeBody impl in higher levels

        return buffer;
    }

    @Override
    public int getSize() {
        return super.getSize() + 2 + 1;
    }
}