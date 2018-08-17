package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.enums.GroupWhisperTarget;
import com.github.manevolent.ts3j.enums.GroupWhisperType;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;

public class PacketBody1VoiceWhisper extends PacketBody {
    private int packetId;
    private int clientId;
    private CodecType codecType;
    private WhisperTarget target;
    private byte[] codecData;

    public PacketBody1VoiceWhisper(ProtocolRole role) {
        super(PacketBodyType.VOICE_WHISPER, role);
    }

    @Override
    public void setHeaderValues(PacketHeader header) {
        if (target instanceof WhisperTargetGroup)
            header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
        else
            header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, false);
    }

    @Override
    public void read(PacketHeader header, ByteBuffer buffer) {
        packetId = buffer.getShort() & 0xFFFF;

        if (getRole() == ProtocolRole.SERVER)
            clientId = buffer.getShort() & 0xFFFF;

        codecType = CodecType.fromId((int) (buffer.get() & 0xFF));


        if (getRole() == ProtocolRole.CLIENT) {
            if (header.getPacketFlag(HeaderFlag.NEW_PROTOCOL)) {
                setTarget(new WhisperTargetGroup());
            } else {
                setTarget(new WhisperTargetMultiple());
            }
        } else {
            setTarget(new WhisperTargetServerToClient());
        }

        target.read(buffer);

        read(buffer);
    }

    // We implemented the above so this is unneccessary.
    @Override
    public void read(ByteBuffer buffer) {
        codecData = new byte[buffer.remaining()];
        buffer.get(codecData);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putShort((short) (packetId & 0xFFFF));

        if (getRole() == ProtocolRole.SERVER)
            buffer.putShort((short) (clientId & 0xFFFF));

        buffer.put((byte)(codecType.getIndex() & 0xFF));

        if (getRole() == ProtocolRole.CLIENT)
            target.write(buffer);

        buffer.put(codecData);
    }

    @Override
    public int getSize() {
        switch (getRole()) {
            case CLIENT:
                return 2 + 1 + target.getSize() + codecData.length;
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

    public WhisperTarget getTarget() {
        return target;
    }

    public void setTarget(WhisperTarget target) {
        this.target = target;
    }

    public static abstract class WhisperTarget {
        public abstract void write(ByteBuffer buffer);
        public abstract void read(ByteBuffer buffer);

        public abstract int getSize();
    }

    public static class WhisperTargetServerToClient extends WhisperTarget {
        @Override
        public void write(ByteBuffer buffer) {
            // Do nothing
        }

        @Override
        public void read(ByteBuffer buffer) {
            // Do nothing
        }

        @Override
        public int getSize() {
            return 0; // Empty
        }
    }

    public static class WhisperTargetMultiple extends WhisperTarget {
        private int[] channelIds;
        private int[] clientIds;

        public int[] getChannelIds() {
            return channelIds;
        }

        public void setChannelIds(int[] channelIds) {
            this.channelIds = channelIds;
        }

        public int[] getClientIds() {
            return clientIds;
        }

        public void setClientIds(int[] clientIds) {
            this.clientIds = clientIds;
        }

        @Override
        public void write(ByteBuffer buffer) {
            if (channelIds != null)
                buffer.put((byte) (channelIds.length & 0xFF));
            else
                buffer.put((byte) 0x00);

            if (clientIds != null)
                buffer.put((byte) (clientIds.length & 0xFF));
            else
                buffer.put((byte) 0x00);

            if (channelIds != null) {
                for (int channelId : channelIds) {
                    buffer.putLong((long) channelId);
                }
            }

            if (clientIds != null) {
                for (int clientId : clientIds) {
                    buffer.putShort((short) (clientId & 0xFFFF));
                }
            }
        }

        @Override
        public void read(ByteBuffer buffer) {
            int channelIdCount = buffer.get() & 0xFF;
            int clientIdCount = buffer.get() & 0xFF;

            channelIds = new int[channelIdCount];
            for (int n = 0; n < channelIdCount; n ++)
                channelIds[n] = (int) (buffer.getLong());

            clientIds = new int[clientIdCount];
            for (int n = 0; n < channelIdCount; n ++)
                clientIds[n] = (buffer.getShort() & 0xFFFF);
        }

        @Override
        public int getSize() {
            return 2 + 2 +
                    (channelIds == null ? 0 : channelIds.length * 8) +
                    (clientIds == null ? 0 : clientIds.length * 2);
        }
    }

    public static class WhisperTargetGroup extends WhisperTarget {
        private GroupWhisperType whisperType;
        private GroupWhisperTarget whisperTarget;
        private long targetId;

        @Override
        public void write(ByteBuffer buffer) {
            buffer.put((byte) (whisperType.getIndex() & 0xFF));
            buffer.put((byte) (whisperTarget.getIndex() & 0xFF));
            buffer.putLong(targetId);
        }

        @Override
        public void read(ByteBuffer buffer) {
            whisperType = GroupWhisperType.fromId(buffer.get() & 0xFF);
            whisperTarget = GroupWhisperTarget.fromId(buffer.get() & 0xFF);
            targetId = buffer.getLong();
        }

        @Override
        public int getSize() {
            return 1 + 1 + 8;
        }

        public GroupWhisperType getWhisperType() {
            return whisperType;
        }

        public void setWhisperType(GroupWhisperType whisperType) {
            this.whisperType = whisperType;
        }

        public GroupWhisperTarget getWhisperTarget() {
            return whisperTarget;
        }

        public void setWhisperTarget(GroupWhisperTarget whisperTarget) {
            this.whisperTarget = whisperTarget;
        }

        public long getTargetId() {
            return targetId;
        }

        public void setTargetId(long targetId) {
            this.targetId = targetId;
        }
    }

    public static WhisperTarget createChannel(int channelId) {
        WhisperTargetMultiple multiple = new WhisperTargetMultiple();
        multiple.setChannelIds(new int[] { channelId });
        return multiple;
    }

    public static WhisperTarget createClient(int clientId) {
        WhisperTargetMultiple multiple = new WhisperTargetMultiple();
        multiple.setClientIds(new int[] { clientId });
        return multiple;
    }

    public static WhisperTarget createServer() {
        WhisperTargetGroup group = new WhisperTargetGroup();

        group.setWhisperTarget(GroupWhisperTarget.ALL_CHANNELS);
        group.setWhisperType(GroupWhisperType.ALL_CLIENTS);

        return group;
    }
}
