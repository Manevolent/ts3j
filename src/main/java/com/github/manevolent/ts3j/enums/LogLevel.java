package com.github.manevolent.ts3j.enums;

public enum LogLevel {
    ERROR(1),
    WARNING(2),
    DEBUG(3),
    INFO(4)

    ;

    private final int index;

    LogLevel(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static LogLevel fromId(int index) {
        for (LogLevel value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
