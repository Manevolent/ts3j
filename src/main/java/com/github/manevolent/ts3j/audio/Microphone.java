package com.github.manevolent.ts3j.audio;

import com.github.manevolent.ts3j.enums.CodecType;

public interface Microphone {
    default boolean isMuted() {
        return false;
    }

    boolean isReady();

    CodecType getCodec();

    byte[] provide();
}
