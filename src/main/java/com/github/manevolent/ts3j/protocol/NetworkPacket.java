package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.nio.ByteBuffer;

/**
 * Represents a fully contained network packet, traveling in or out.
 */
public final class NetworkPacket {
    private final ProtocolRole role;

    private PacketHeader header;
    private Packet packet;

    public NetworkPacket(ProtocolRole role) {
        if (role == null) throw new NullPointerException("role");

        this.role = role;
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

    public Packet getPacket() {
        return packet;
    }

    public void setPacket(Packet packet) {
        if (packet == null) throw new NullPointerException("packet");

        this.packet = packet;
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

        getHeader().setType(getPacket().getType());

        getHeader().write(buffer);

        return buffer;
    }

    public ByteBuffer writeBody(ByteBuffer buffer) {
        if (getPacket() == null) throw new NullPointerException("packet");

        getPacket().write(buffer);

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
        if (getPacket() == null) {
            try {
                setPacket(
                        getHeader().getType().getPacketClass()
                        .getConstructor(ProtocolRole.class)
                        .newInstance(getHeader().getRole())
                );
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        getPacket().read(buffer);

        return buffer;
    }

    /**
     * Gets the size of this packet.  This method is generally only used when writing a fully defined packet.
     * @return packet size.
     */
    public int getSize() {
        return getHeader().getSize() + getPacket().getSize();
    }
}
