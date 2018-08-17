package com.github.manevolent.ts3j.enums;

public enum GroupWhisperType {
    SERVER_GROUP(0),
    CHANNEL_GROUP(1),
    CHANNEL_COMMANDER(2),
    ALL_CLIENTS(3)

    ;

    private final int index;

    GroupWhisperType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static GroupWhisperType fromId(int index) {
        for (GroupWhisperType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
