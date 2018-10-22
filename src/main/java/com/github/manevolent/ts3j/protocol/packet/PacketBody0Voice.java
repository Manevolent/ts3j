package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.nio.ByteBuffer;

public class PacketBody0Voice extends PacketBody {
    private int packetId;
    private int clientId;
    private CodecType codecType;
    private byte[] codecData;

    // Unknown at this time (have seen: 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
    // Set for the first 5 packets of a new voice session
    // I believe this is related to tracking opening a new decoder
    // Not all new voice sessions will change this flag
    // Best to ignore it for now, but it's here if you need it or figure it out
    private Byte serverFlag0;

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

    public Byte getServerFlag0() { return serverFlag0; }

    public void setServerFlag0(Byte flag) {
        this.serverFlag0 = flag;
    }

    public void setClientId(int clientId) {
        if (getRole() != ProtocolRole.SERVER)
            throw new IllegalStateException("cannot set client ID field on non-server packet");

        this.clientId = clientId;
    }

    @Override
    public void setHeaderValues(PacketHeader header) {
        if (serverFlag0 != null) // Ensure padding is signaled
            header.setPacketFlag(HeaderFlag.COMPRESSED, true);
    }

    @Override
    public void read(PacketHeader header, ByteBuffer buffer) {
        super.read(header, buffer);

        int padding = 0;
        boolean compressed = header.getPacketFlag(HeaderFlag.COMPRESSED);

        if (compressed)
            padding ++;

        codecData = new byte[buffer.remaining() - padding];
        buffer.get(codecData);

        // Handle trailing data
        if (padding > 0) {
            if (compressed)
                serverFlag0 = buffer.get();
        }
    }

    @Override
    public void read(ByteBuffer buffer) {
        packetId = buffer.getShort() & 0x0000FFFF;

        if (getRole() == ProtocolRole.SERVER)
            clientId = buffer.getShort() & 0x0000FFFF;

        codecType = CodecType.fromId(buffer.get() & 0xFF);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (packetId & 0xFFFF));

        if (getRole() == ProtocolRole.SERVER)
            buffer.putShort((short) (clientId & 0xFFFF));

        buffer.put((byte)(codecType.getIndex() & 0xFF));

        buffer.put(codecData);

        if (serverFlag0 != null)
            buffer.put(serverFlag0);
    }

    @Override
    public int getSize() {
        return 2 +
                (getRole() == ProtocolRole.SERVER ? 2 : 0) +
                1 +
                codecData.length +
                (serverFlag0 != null ? 1 : 0);
    }
}
