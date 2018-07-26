package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.command.SimpleCommand;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class PacketBody2Command extends PacketBody {
    private String text;

    public PacketBody2Command(ProtocolRole role) {
        super(PacketBodyType.COMMAND, role);
    }

    public String getText() {
        return text;
    }

    public SimpleCommand parse() {
        return SimpleCommand.parse(getRole(), getText());
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
