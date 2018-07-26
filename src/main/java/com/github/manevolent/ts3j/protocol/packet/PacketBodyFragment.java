package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;

public class PacketBodyFragment extends PacketBody {
    private byte[] raw;

    public PacketBodyFragment(PacketBodyType type, ProtocolRole role) {
        super(type, role);
    }

    public PacketBodyFragment(PacketBodyType type, ProtocolRole role, byte[] body) {
        super(type, role);

        this.raw = body;
    }

    public byte[] getRaw() {
        return raw;
    }

    public void setRaw(byte[] raw) {
        this.raw = raw;
    }

    @Override
    public void read(ByteBuffer buffer) {
        raw = new byte[buffer.remaining()];
        buffer.get(raw);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(raw);
    }

    @Override
    public int getSize() {
        return raw.length;
    }
}
