package com.github.manevolent.ts3j.enums;

public enum PacketFlags {
    NONE(0x0),
    FRAGMENTED(0X10),
    NEW_PROTOCOL(0x20),
    COMPRESSED(0x40),
    UNENCRYPTED(0x80);

    private final int index;

    PacketFlags(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static PacketFlags fromId(int index) {
        for (PacketFlags value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
