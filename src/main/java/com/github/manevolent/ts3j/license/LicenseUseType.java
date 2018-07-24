package com.github.manevolent.ts3j.license;

public enum LicenseUseType {
    INTERMEDIATE(0, IntermediateLicenseUse.class),
    SERVER(2, ServerLicenseUse.class),
    EPHEMERAL(32, EphemeralLicenseUse.class)
    ;

    private final int index;
    private final Class<? extends LicenseUse> use;

    LicenseUseType(int index, Class<? extends LicenseUse> use) {
        this.index = index;
        this.use = use;
    }

    public int getIndex() {
        return index;
    }

    public static LicenseUseType fromId(int index) {
        for (LicenseUseType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }

    public Class<? extends LicenseUse> getUseClass() {
        return use;
    }
}
