package com.github.manevolent.ts3j.enums;

public enum GroupType {
    TEMPLATE(0),
    REGULAR(1),
    QUERY(2)

    ;

    private final int index;

    GroupType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static GroupType fromId(int index) {
        for (GroupType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
