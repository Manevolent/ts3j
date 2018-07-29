package com.github.manevolent.ts3j.identity;

import java.util.Arrays;
import java.util.Base64;

public final class Uid {
    private final byte[] uidBytes;
    private final String base64;

    public Uid(String base64) {
        this.base64 = base64;
        this.uidBytes = Base64.getDecoder().decode(base64);
    }

    public Uid(byte[] bytes) {
        this.uidBytes = bytes;
        this.base64 = Base64.getEncoder().encodeToString(bytes);
    }

    public String toBase64() {
        return base64;
    }

    public byte[] toByteArray() {
        return uidBytes;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toByteArray());
    }

    @Override
    public boolean equals(Object b) {
        if (b == null || !(b instanceof Uid)) return false;
        return equals((Uid) b);
    }

    public boolean equals(Uid b) {
        return b != null && Arrays.equals(b.toByteArray(), toByteArray());
    }
}
