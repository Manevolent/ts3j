package com.github.manevolent.ts3j.enums;

public enum PluginTargetMode {
    CURRENT_CHANNEL(0),
    SERVER(1),
    CLIENT(2),
    CURRENT_CHANNEL_SUBSCRIBED_CLIENTS(3)

    ;

    private final int index;

    PluginTargetMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static PluginTargetMode fromId(int index) {
        for (PluginTargetMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
