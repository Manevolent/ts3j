package com.github.manevolent.ts3j.enums;

public enum KickIdentifier {
    CHANNEL(4),
    SERVER(5)

    ;

    private final int index;

    KickIdentifier(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static KickIdentifier fromId(int index) {
        for (KickIdentifier value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
