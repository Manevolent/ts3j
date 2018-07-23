package com.github.manevolent.ts3j.enums;

public enum GroupNamingMode {
    UID(1 << 0),
    AWAY(1 << 1),
    VOICE(1 << 2),
    TIMES(1 << 3),
    GROUPS(1 << 4),
    INFO(1 << 5),
    ICON(1 << 6),
    COUNTRY(1 << 7);

    private final int index;

    GroupNamingMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static GroupNamingMode fromId(int index) {
        for (GroupNamingMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
