package com.github.manevolent.ts3j.license;

import com.github.manevolent.ts3j.enums.LicenseType;

import java.nio.ByteBuffer;

public abstract class LicenseUse {
    protected LicenseUse() {
    }

    public abstract int getSize();

    public abstract LicenseUseType getUseType();

    public abstract LicenseType getLicenseType();

    public abstract ByteBuffer write(ByteBuffer buffer);

    public abstract ByteBuffer read(ByteBuffer buffer);
}
