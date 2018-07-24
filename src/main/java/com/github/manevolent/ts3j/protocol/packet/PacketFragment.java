package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class PacketFragment extends Packet {
    private byte[] fragment;

    public PacketFragment( PacketType type, ProtocolRole role) {
        super(type, role);
    }

    public byte[] getFragment() {
        return fragment;
    }

    public void setFragment(byte[] fragment) {
        this.fragment = fragment;
    }

    @Override
    public void read(ByteBuffer buffer) {
        fragment = new byte[buffer.remaining()];
        buffer.get(fragment);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(fragment);
    }

    @Override
    public int getSize() {
        return fragment.length;
    }
}
