package com.github.manevolent.ts3j.enums;

public enum VoiceEncryptionMode {
    INDIVIDUAL(0),
    DISABLED(1),
    ENABLED(2)

    ;

    private final int index;

    VoiceEncryptionMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static VoiceEncryptionMode fromId(int index) {
        for (VoiceEncryptionMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
