package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody;

import java.nio.ByteBuffer;

/**
 * Represents a fully contained network packet, traveling in or out.
 */
public final class Packet {
    private final ProtocolRole role;

    private PacketHeader header;
    private PacketBody body;

    public Packet(ProtocolRole role) {
        if (role == null) throw new NullPointerException("role");

        this.role = role;
    }

    public Packet(ProtocolRole role, PacketHeader header) {
        if (role == null) throw new NullPointerException("role");

        this.role = role;
        this.header = header;
    }

    public ProtocolRole getRole() {
        return role;
    }

    public PacketHeader getHeader() {
        return header;
    }

    public void setHeader(PacketHeader header) {
        if (header == null) throw new NullPointerException("header");

        this.header = header;
    }

    public PacketBody getBody() {
        return body;
    }

    public void setBody(PacketBody packetBody) {
        if (packetBody == null) throw new NullPointerException("packet");

        this.body = packetBody;
    }

    /**
     * Writes the existing data out to the specified byte buffer
     * @param buffer Buffer to write to
     */
    public ByteBuffer write(ByteBuffer buffer) {
        writeHeader(buffer);
        writeBody(buffer);

        return buffer;
    }

    public ByteBuffer writeHeader(ByteBuffer buffer) {
        if (getHeader() == null) throw new NullPointerException("header");

        if (getBody() != null)
            getHeader().setType(getBody().getType());

        getHeader().write(buffer);

        return buffer;
    }

    public ByteBuffer writeBody(ByteBuffer buffer) {
        if (getBody() == null) throw new NullPointerException("packet");

        getBody().write(buffer);

        return buffer;
    }

    /**
     * Reads data from the specified byte buffer, filling in the packet body as needed.
     * @param buffer Buffer to read from.
     */
    public ByteBuffer read(ByteBuffer buffer) {
        readHeader(buffer);
        readBody(buffer);

        return buffer;
    }

    public ByteBuffer readHeader(ByteBuffer buffer) {
        if (getHeader() == null) {
            try {
                setHeader(getRole().getHeaderClass().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        getHeader().read(buffer);

        return buffer;
    }

    public ByteBuffer readBody(ByteBuffer buffer) {
        if (getBody() == null) {
            try {
                setBody(
                        getHeader().getType().getPacketClass()
                                .getConstructor(ProtocolRole.class)
                                .newInstance(getHeader().getRole())
                );
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        getBody().read(buffer);

        return buffer;
    }

    /**
     * Gets the size of this packet.  This method is generally only used when writing a fully defined packet.
     * @return packet size.
     */
    public int getSize() {
        return (getHeader() == null ? 0 : getHeader().getSize()) + (getBody() == null ? 0 : getBody().getSize());
    }
}
