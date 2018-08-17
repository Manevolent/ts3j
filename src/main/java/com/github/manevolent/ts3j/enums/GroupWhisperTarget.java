package com.github.manevolent.ts3j.enums;

public enum GroupWhisperTarget {
    ALL_CHANNELS(0),
    CURRENT_CHANNEL(1),
    PARENT_CHANNEL(2),
    ALL_PARENT_CHANNEL(3),
    CHANNEL_FAMILY(4),
    COMPLETE_CHANNEL_FAMILY(5),
    SUBCHANNELS(6)

    ;

    private final int index;

    GroupWhisperTarget(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static GroupWhisperTarget fromId(int index) {
        for (GroupWhisperTarget value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
