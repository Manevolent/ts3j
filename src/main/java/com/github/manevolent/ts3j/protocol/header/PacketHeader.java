package com.github.manevolent.ts3j.protocol.header;

import com.github.manevolent.ts3j.protocol.packet.PacketType;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public abstract class PacketHeader {
    private final ProtocolRole role;

    //private final byte[] DEFAULT_MAC = new byte[] {0x0,0x0,0x0,0x0,0x0,0x0,0x0,0x0}; // 8x0's
    private byte[] mac = new byte[8]; // 8 bytes
    private int packetId = 0;
    private PacketType type;
    private int packetFlags;

    protected PacketHeader(ProtocolRole role) {
        this.role = role;
    }

    public int getPacketFlags() {
        return packetFlags;
    }

    public void setPacketFlags(int packetFlags) {
        this.packetFlags = packetFlags;
    }

    public boolean getPacketFlag(HeaderFlag flag) {
        int idx = flag.getIndex();

        return (packetFlags & idx) == idx;
    }

    public void setPacketFlag(HeaderFlag flag, boolean value) {
        int idx = flag.getIndex();

        if (value) {
            packetFlags = packetFlags | idx;
        } else {
            packetFlags &= ~idx;
        }
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public byte[] getMac() {
        return mac;
    }

    public void setMac(byte[] mac) {
        Packet.assertArray("mac", mac, 8);

        this.mac = mac;
    }

    public final void write(ByteBuffer buffer) {
        writeHeader(buffer);
    }

    public final void read(ByteBuffer buffer) {
        readHeader(buffer);
    }

    protected void writeHeader(ByteBuffer buffer) {
        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md
        buffer.put(getMac(), 0, 8);
        buffer.putShort((short) (getPacketId() & 0x0000FFFF)); // PiD / Packet ID
    }

    protected void readHeader(ByteBuffer buffer) {
        // https://github.com/ReSpeak/tsdeclarations/blob/master/ts3protocol.md
        buffer.get(mac);
        setPacketId((int) buffer.getShort() & 0x0000FFFF);
    }

    public int getSize() {
        return 8 + 2;
    }

    public ProtocolRole getRole() {
        return role;
    }
}
