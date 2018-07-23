package com.github.manevolent.ts3j.enums;

public enum LicenseType {
    NO_LICENSE(0),
    ATHP(1),
    LAN(2),
    NPL(3),
    UNKNOWN(4)

    ;

    private final int index;

    LicenseType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static LicenseType fromId(int index) {
        for (LicenseType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
