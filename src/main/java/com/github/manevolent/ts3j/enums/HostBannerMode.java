package com.github.manevolent.ts3j.enums;

public enum HostBannerMode {
    NO_ADJUST(0),
    IGNORE_ASPECT_RATIO(1),
    KEEP_ASPECT(2)

    ;

    private final int index;

    HostBannerMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static HostBannerMode fromId(int index) {
        for (HostBannerMode value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
