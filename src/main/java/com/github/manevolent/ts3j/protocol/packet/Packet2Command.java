package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Packet2Command extends Packet {
    private String text;

    public Packet2Command(ProtocolRole role) {
        super(PacketType.COMMAND, role);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void read(ByteBuffer buffer) {
        text = Charset.forName("ASCII").decode(buffer).toString();
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(text.getBytes(Charset.forName("ASCII")));
    }

    @Override
    public int getSize() {
        return text.length();
    }
}
