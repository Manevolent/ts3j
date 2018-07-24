package com.github.manevolent.ts3j.license;

import com.github.manevolent.ts3j.enums.LicenseType;

import java.nio.ByteBuffer;

import static com.github.manevolent.ts3j.enums.LicenseType.NO_LICENSE;
import static com.github.manevolent.ts3j.enums.LicenseType.UNKNOWN;

public class IntermediateLicenseUse extends LicenseUse {
    private byte[] unknown = new byte[4];
    private String issuer;

    public IntermediateLicenseUse() {
        super();
    }

    @Override
    public int getSize() {
        return 4 + issuer.length() + 1;
    }

    @Override
    public LicenseUseType getUseType() {
        return LicenseUseType.EPHEMERAL;
    }

    @Override
    public LicenseType getLicenseType() {
        return UNKNOWN;
    }

    @Override
    public ByteBuffer write(ByteBuffer buffer) {
        buffer.put(unknown);
        License.writeNullTerminatedLicenseString(buffer, issuer);

        return buffer;
    }

    @Override
    public ByteBuffer read(ByteBuffer buffer) {
        buffer.get(unknown);

        issuer = License.readNullTerminatedLicenseString(buffer);

        return buffer;
    }

    public byte[] getUnknown() {
        return unknown;
    }

    public void setUnknown(byte[] unknown) {
        this.unknown = unknown;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
