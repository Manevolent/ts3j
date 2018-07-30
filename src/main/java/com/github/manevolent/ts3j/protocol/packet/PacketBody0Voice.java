package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class PacketBody0Voice extends PacketBody {
    private int packetId;
    private int clientId;
    private CodecType codecType;
    private byte[] codecData;

    public PacketBody0Voice(ProtocolRole role) {
        super(PacketBodyType.VOICE, role);
    }

    public int getPacketId() {
        return packetId;
    }

    public void setPacketId(int packetId) {
        this.packetId = packetId;
    }

    public CodecType getCodecType() {
        return codecType;
    }

    public void setCodecType(CodecType codecType) {
        this.codecType = codecType;
    }

    public byte[] getCodecData() {
        return codecData;
    }

    public void setCodecData(byte[] codecData) {
        this.codecData = codecData;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        if (getRole() != ProtocolRole.SERVER)
            throw new IllegalStateException("cannot set client ID field on non-server packet");

        this.clientId = clientId;
    }

    @Override
    public void read(ByteBuffer buffer) {
        packetId = buffer.getShort() & 0x0000FFFF;

        if (getRole() == ProtocolRole.SERVER)
            clientId = buffer.getShort() & 0x0000FFFF;

        codecType = CodecType.fromId((int) (buffer.get() & 0xFF));

        codecData = new byte[buffer.remaining()];

        buffer.get(codecData);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (packetId & 0xFFFF));

        if (getRole() == ProtocolRole.SERVER)
            buffer.putShort((short) (clientId & 0xFFFF));

        buffer.put((byte)(codecType.getIndex() & 0xFF));

        buffer.put(codecData);
    }

    @Override
    public int getSize() {
        return 2 + (getRole() == ProtocolRole.SERVER ? 2 : 0) + 1 + codecData.length;
    }
}
