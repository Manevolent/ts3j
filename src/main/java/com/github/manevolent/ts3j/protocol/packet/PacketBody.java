package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;

public abstract class PacketBody {
    private final PacketBodyType type;
    private final ProtocolRole role;

    protected PacketBody(PacketBodyType type, ProtocolRole role) {
        this.type = type;
        this.role = role;
    }

    public PacketBodyType getType() {
        return type;
    }

    public ProtocolRole getRole() {
        return role;
    }

    public abstract void read(ByteBuffer buffer);

    public abstract void write(ByteBuffer buffer);

    public abstract int getSize();

    public void setHeaderValues(PacketHeader header) {

    }

    /**
     * Asserts that an array length is correct
     * @param field Field name being asserted
     * @param bytes Bytes being set
     * @param length Length of the byte array expected
     * @throws IllegalArgumentException
     */
    public static final void assertArray(String field, byte[] bytes, int length) throws IllegalArgumentException {
        if (bytes == null)
            throw new NullPointerException(field + " attempted set to null pointer");

        if (bytes.length != length)
            throw new IllegalArgumentException(field + " len " + bytes.length + " != " + length);
    }
}
