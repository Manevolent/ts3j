package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PacketBody1VoiceWhisper extends PacketBody {
    private int packetId;
    private int clientId;
    private CodecType codecType;
    private byte[] codecData;

    public PacketBody1VoiceWhisper(ProtocolRole role) {
        super(PacketBodyType.VOICE_WHISPER, role);
    }

    @Override
    public void read(PacketHeader header, ByteBuffer buffer) {
        packetId = buffer.getShort() & 0x0000FFFF;

        if (getRole() == ProtocolRole.SERVER)
            clientId = buffer.getShort() & 0x0000FFFF;

        codecType = CodecType.fromId((int) (buffer.get() & 0xFF));


        if (getRole() == ProtocolRole.CLIENT) {
            if (header.getPacketFlag(HeaderFlag.NEW_PROTOCOL)) {
                int groupWhisperType = buffer.get() & 0xFF;
                int groupWhisperTarget = buffer.get() & 0xFF;
                long targetChannelIdorGroupId = buffer.getLong();

                // TODO
            } else {
                int channelIdCount = buffer.get() & 0xFF;
                int clientIdCount = buffer.get() & 0xFF;

                List<Long> channelIds = new ArrayList<>(channelIdCount);
                for (int i = 0; i < channelIdCount; i ++) channelIds.add(buffer.getLong());

                List<Integer> clientIds = new ArrayList<>(clientIdCount);
                for (int i = 0; i < clientIdCount; i ++) clientIds.add(buffer.getShort() & 0xFFFF);

                // TODO
            }
        }

        codecData = new byte[buffer.remaining()];

        buffer.get(codecData);
    }

    @Override
    public void read(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
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
        switch (getRole()) {
            case CLIENT:
                // TODO
                throw new UnsupportedOperationException();
            case SERVER:
                return 2 + 2 + 1 + codecData.length;
            default:
                throw new IllegalArgumentException("unknown role");
        }
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
}
