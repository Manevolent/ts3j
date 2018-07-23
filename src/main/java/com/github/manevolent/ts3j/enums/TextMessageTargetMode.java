package com.github.manevolent.ts3j.enums;

public enum TextMessageTargetMode {
    PRIVATE(1),
    CHANNEL(2),
    SERVER(3)

    ;

    private final int index;

    TextMessageTargetMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static TextMessageTargetMode fromId(int index) {
        for (TextMessageTargetMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
