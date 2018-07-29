package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBody3CommandLow;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class LocalClientHandlerFull extends LocalClientHandler {
    protected LocalClientHandlerFull(LocalTeamspeakClientSocket client) {
        super(client);
    }

    protected void handleCommand(MultiCommand command) throws CommandProcessException {
        CommandProcessor processor = getClient().getCommandProcessor();
        if (processor != null) processor.process(getClient(), command);
    }

    @Override
    public void handlePacket(Packet packet) throws IOException, TimeoutException {
        switch (packet.getBody().getType()) {
            case COMMAND:
                Ts3Debugging.debug(((PacketBody2Command) packet.getBody()).getText());
                try {
                    handleCommand(MultiCommand.parse(
                            packet.getRole(),
                            ((PacketBody2Command) packet.getBody()).getText()
                    ));
                } catch (CommandProcessException e) {
                    throw new IOException(e);
                }
                break;
            case COMMAND_LOW:
                try {
                    handleCommand(MultiCommand.parse(
                            packet.getRole(),
                            ((PacketBody3CommandLow) packet.getBody()).getText()
                    ));
                } catch (CommandProcessException e) {
                    throw new IOException(e);
                }
                break;
        }
    }
}
