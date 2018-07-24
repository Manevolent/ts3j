package com.github.manevolent.ts3j.license;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class License {
    public static final byte[] ROOT_KEY =
            {
                    (byte) 0xcd, 0x0d, (byte) 0xe2, (byte) 0xae, (byte) 0xd4, 0x63, 0x45, 0x50, (byte) 0x9a,
                    0x7e, 0x3c, (byte) 0xfd, (byte) 0x8f, 0x68, (byte) 0xb3, (byte) 0xdc, 0x75, 0x55, (byte) 0xb2,
                    (byte) 0x9d, (byte) 0xcc, (byte) 0xec, 0x73, (byte) 0xcd, 0x18, 0x75, 0x0f, (byte) 0x99,
                    0x38, 0x12, 0x40, (byte) 0x8a
            };

    private byte[] computedHash;

    private byte[] publicKey = new byte[32];
    private byte licenseBlockType;
    private long start,
                 end;

    private LicenseUse use;

    public LicenseUse getUse() {
        return use;
    }

    public void setUse(LicenseUse use) {
        this.use = use;
        this.licenseBlockType = (byte) (use.getUseType().getIndex() & 0xFF);
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte getLicenseBlockType() {
        return licenseBlockType;
    }

    public void setLicenseBlockType(byte licenseBlockType) {
        this.licenseBlockType = licenseBlockType;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getSize() {
        return 32 + 1 + 8 + use.getSize();
    }

    public ByteBuffer write(ByteBuffer buffer) {
        writeHeader(buffer);
        writeBody(buffer);

        return buffer;
    }

    public ByteBuffer read(ByteBuffer buffer) {
        readHeader(buffer);
        readBody(buffer);

        return buffer;
    }


    public ByteBuffer readHeader(ByteBuffer buffer) {
        byte keyType = buffer.get();
        if (keyType != 0x00) throw new IllegalArgumentException("invalid key type: " + keyType);

        buffer.get(publicKey);
        licenseBlockType = buffer.get();
        start = buffer.getInt() & 0x000000FFFFFF;
        end = buffer.getInt() & 0x000000FFFFFF;

        return buffer;
    }

    public ByteBuffer readBody(ByteBuffer buffer) {
        LicenseUseType useType = LicenseUseType.fromId(getLicenseBlockType());

        try {
            use = useType.getUseClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        use.read(buffer);

        return buffer;
    }


    public ByteBuffer writeHeader(ByteBuffer buffer) {
        buffer.put((byte) 0x00);
        buffer.put(publicKey);
        buffer.put((byte) (licenseBlockType & 0xFF));
        buffer.putInt((int) (start & 0x000000FFFFFF));
        buffer.putInt((int) (end & 0x000000FFFFFF));

        return buffer;
    }

    public ByteBuffer writeBody(ByteBuffer buffer) {
        use.write(buffer);

        return buffer;
    }

    public static String readNullTerminatedLicenseString(ByteBuffer buffer) {
        StringBuilder builder = new StringBuilder();
        while (buffer.remaining() > 0) {
            char c = (char) buffer.get();
            switch (c) {
                case '\0':
                    break;
                default:
                    builder.append(c);
            }
        }
        return builder.toString();
    }

    public static void writeNullTerminatedLicenseString(ByteBuffer buffer, String s) {
        buffer.put((s + "\0").getBytes(Charset.forName("ASCII")));
    }

    public static List<License> readLicenses(ByteBuffer buffer) {
        List<License> licenses = new ArrayList<>();

        while (buffer.remaining() > 0) {
            byte licenseVersion = buffer.get();
            if (licenseVersion != 0x01)
                throw new IllegalArgumentException("invalid license version");

            License license = new License();
            license.read(buffer);
            licenses.add(license);
        }

        return licenses;
    }

    public byte[] getHash() {
        if (computedHash == null) {
            ByteBuffer buffer = ByteBuffer.allocate(getSize());
            write(buffer);

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA512");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            return digest.digest(buffer.array());
        }

        return computedHash;
    }
}
