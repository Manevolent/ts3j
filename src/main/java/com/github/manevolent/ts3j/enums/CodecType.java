package com.github.manevolent.ts3j.enums;

public enum CodecType {
    SPEEX_NARROWBAND(0),
    SPEEX_WIDEBAND(1),
    SPEEX_ULTRA_WIDEBAND(2),
    CELT_MONO(3),
    OPUS_VOICE(4),
    OPUS_MUSIC(5),

    RAW(127)

    ;

    private final int index;

    CodecType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static CodecType fromId(int index) {
        for (CodecType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
