package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public final class NetworkPacket {
    private final DatagramPacket datagram;
    private final PacketHeader header;
    private final ByteBuffer buffer;

    public NetworkPacket(DatagramPacket datagram, PacketHeader header, ByteBuffer buffer) {
        this.datagram = datagram;
        this.header = header;
        this.buffer = buffer;
    }

    /**
     * Gets the original packet datagram.
     * @return Packet datagram.
     */
    public DatagramPacket getDatagram() {
        return datagram;
    }

    /**
     * Gets the received header.
     * @return Packet header.
     */
    public PacketHeader getHeader() {
        return header;
    }

    /**
     * Gets the original packet buffer.
     * @return Packet buffer.
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }
}
