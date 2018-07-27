package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.ComplexCommand;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class LocalClientHandlerFull extends LocalClientHandler {
    protected LocalClientHandlerFull(LocalTeamspeakClientSocket client) {
        super(client);
    }

    protected void handleCommand(ComplexCommand command) throws CommandProcessException {
        CommandProcessor processor = getClient().getCommandProcessor();
        if (processor != null) processor.process(getClient(), command);
    }

    @Override
    public void handlePacket(Packet packet) throws IOException, TimeoutException {
        if (packet.getBody() instanceof PacketBody2Command)
            try {
                handleCommand(ComplexCommand.parse(packet.getRole(), ((PacketBody2Command) packet.getBody()).getText()));
            } catch (CommandProcessException e) {
                throw new IOException(e);
            }
    }
}
