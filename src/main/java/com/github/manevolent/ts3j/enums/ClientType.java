package com.github.manevolent.ts3j.enums;

public enum ClientType {
    FULL(0),
    QUERY(1);

    private final int index;

    ClientType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static ClientType fromId(int index) {
        for (ClientType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
