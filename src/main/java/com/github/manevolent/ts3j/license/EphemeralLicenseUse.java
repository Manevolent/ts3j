package com.github.manevolent.ts3j.license;

import com.github.manevolent.ts3j.enums.LicenseType;

import java.nio.ByteBuffer;

public class EphemeralLicenseUse extends LicenseUse {
    public EphemeralLicenseUse() {
        super();
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public LicenseUseType getUseType() {
        return LicenseUseType.EPHEMERAL;
    }

    @Override
    public LicenseType getLicenseType() {
        return LicenseType.UNKNOWN;
    }

    @Override
    public ByteBuffer write(ByteBuffer buffer) {
        return buffer;
    }

    @Override
    public ByteBuffer read(ByteBuffer buffer) {
        return buffer;
    }
}
