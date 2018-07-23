package com.github.manevolent.ts3j.protocol.packet;

public enum PacketType {
    VOICE(0x0, null),
    VOICE_WHISPER(0x1, null),
    COMMAND(0x2, null),
    COMMAND_LOW(0x3, null),
    PING(0x4, null),
    PONG(0x5, null),
    ACK(0x6, null),
    ACK_LOW(0x7, null),
    INIT1(0x8, Packet8Init1.class)
    ;

    private final int index;
    private final Class<? extends Packet> packetClass;

    PacketType(int index, Class<? extends Packet> packetClass) {
        this.index = index;
        this.packetClass = packetClass;
    }

    public int getIndex() {
        return index;
    }

    public static PacketType fromId(int index) {
        for (PacketType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }

    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }
}
