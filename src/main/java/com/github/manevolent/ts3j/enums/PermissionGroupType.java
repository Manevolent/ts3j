package com.github.manevolent.ts3j.enums;

public enum PermissionGroupType {
    SERVER_GROUP(0),
    GLOBAL_CLIENT(1),
    CHANNEL(2),
    CHANNEL_GROUP(3),
    CHANNEL_CLIENT(4)

    ;

    private final int index;

    PermissionGroupType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static PermissionGroupType fromId(int index) {
        for (PermissionGroupType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
