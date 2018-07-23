package com.github.manevolent.ts3j.enums;

public enum ClientListOptions {
    UID(1 << 0),
    AWAY(1 << 1),
    VOICE(1 << 2),
    TIMES(1 << 3),
    GROUPS(1 << 4),
    INFO(1 << 5),
    ICON(1 << 6),
    COUNTRY(1 << 7);

    private final int index;

    ClientListOptions(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static ClientListOptions fromId(int index) {
        for (ClientListOptions value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
