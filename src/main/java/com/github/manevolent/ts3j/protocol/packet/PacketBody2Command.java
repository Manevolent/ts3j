package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class PacketBody2Command extends PacketBody {
    private String text;

    public PacketBody2Command(ProtocolRole role) {
        super(PacketBodyType.COMMAND, role);
    }

    public PacketBody2Command(ProtocolRole role, String command) {
        this(role);

        this.text = command;
    }

    public PacketBody2Command(ProtocolRole role, Command command) {
        this(role, command.build());
    }

    public String getText() {
        return text;
    }

    public MultiCommand parse() {
        return MultiCommand.parse(getRole(), getText());
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public void read(ByteBuffer buffer) {
        text = Charset.forName("UTF8").decode(buffer).toString();
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(text.getBytes(Charset.forName("UTF8")));
    }

    @Override
    public int getSize() {
        return text.getBytes(Charset.forName("UTF8")).length;
    }
}
