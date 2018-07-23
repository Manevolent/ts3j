package com.github.manevolent.ts3j.enums;

public enum PacketType {
    VOICE(0x0),
    VOICE_WHISPER(0x1),
    COMMAND(0x2),
    COMMAND_LOW(0x3),
    PING(0x4),
    PONG(0x5),
    ACK(0x6),
    ACK_LOW(0x7),
    INIT(0x8)
    ;

    private final int index;

    PacketType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static PacketType fromId(int index) {
        for (PacketType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
