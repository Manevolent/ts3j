package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class PacketBodyCompressed extends PacketBody {
    private byte[] compressed;

    public PacketBodyCompressed(PacketBodyType type, ProtocolRole role) {
        super(type, role);
    }

    public PacketBodyCompressed(PacketBodyType type, ProtocolRole role, byte[] body) {
        super(type, role);

        this.compressed = body;
    }

    public byte[] getCompressed() {
        return compressed;
    }

    public void setCompressed(byte[] compressed) {
        this.compressed = compressed;
    }

    @Override
    public void read(ByteBuffer buffer) {
        compressed = new byte[buffer.remaining()];
        buffer.get(compressed);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(compressed);
    }

    @Override
    public int getSize() {
        return compressed.length;
    }
}
