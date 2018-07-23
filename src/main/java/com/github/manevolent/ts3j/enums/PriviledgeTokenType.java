package com.github.manevolent.ts3j.enums;

public enum PriviledgeTokenType {
    SERVER_GROUP(0),
    CHANNEL_GROUP(1)

    ;

    private final int index;

    PriviledgeTokenType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static PriviledgeTokenType fromId(int index) {
        for (PriviledgeTokenType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
