package com.github.manevolent.ts3j.enums;

public enum GroupWhisperType {
    ALL_CHANNELS(0),
    CURRENT_CHANNEL(1),
    ALL_PARENT_CHANNEL(2),
    CHANNEL_FAMILY(3),
    COMPLETE_CHANNEL_FAMILY(4),
    SUBCHANNELS(5)

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
