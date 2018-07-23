package com.github.manevolent.ts3j.protocol.header;

public enum HeaderFlag {
    NONE(0x0),
    FRAGMENTED(0X10),
    NEW_PROTOCOL(0x20),
    COMPRESSED(0x40),
    UNENCRYPTED(0x80);

    private final int index;

    HeaderFlag(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static HeaderFlag fromId(int index) {
        for (HeaderFlag value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
