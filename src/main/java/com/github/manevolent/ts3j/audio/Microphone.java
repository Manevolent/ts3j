package com.github.manevolent.ts3j.audio;

public interface Microphone {
    default boolean isMuted() {
        return false;
    }

    boolean isReady();

    default AudioEncoding getEncoding() {
        return AudioEncoding.PCM;
    }

    byte[] provide();
}
