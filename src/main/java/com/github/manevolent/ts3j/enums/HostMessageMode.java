package com.github.manevolent.ts3j.enums;

public enum HostMessageMode {
    NONE(0),
    LOG(1),
    MODAL(2),
    MODAL_QUIT(3)

    ;

    private final int index;

    HostMessageMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static HostMessageMode fromId(int index) {
        for (HostMessageMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
