package com.github.manevolent.ts3j.license;

import com.github.manevolent.ts3j.enums.LicenseType;

import java.nio.ByteBuffer;

public class ServerLicenseUse extends LicenseUse {
    private byte type;
    private byte[] unknown = new byte[4];
    private String issuer;

    public ServerLicenseUse() {
        super();
    }

    @Override
    public int getSize() {
        return 1 + 4 + issuer.length() + 1;
    }

    @Override
    public LicenseUseType getUseType() {
        return LicenseUseType.SERVER;
    }

    @Override
    public ByteBuffer write(ByteBuffer buffer) {
        buffer.put(type);
        buffer.put(unknown);
        License.writeNullTerminatedLicenseString(buffer, issuer);

        return buffer;
    }

    @Override
    public ByteBuffer read(ByteBuffer buffer) {
        type = buffer.get();
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

    public LicenseType getLicenseType() {
        return LicenseType.fromId(type);
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }
}
