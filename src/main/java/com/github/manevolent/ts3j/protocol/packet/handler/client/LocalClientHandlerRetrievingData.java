package com.github.manevolent.ts3j.protocol.packet.handler.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;


public class LocalClientHandlerRetrievingData extends LocalClientHandlerFull {
    public LocalClientHandlerRetrievingData(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    protected void handleCommand(MultiCommand command) throws CommandProcessException {
        if (command.getName().equalsIgnoreCase("error")) {
            try {
                getClient().setState(ClientConnectionState.DISCONNECTED);
            } catch (Exception e) {
                getClient().getExceptionHandler().accept(e);
            }

            throw new CommandProcessException(command.simplifyOne().get("msg").getValue());
        }

        CommandProcessor processor = getClient().getCommandProcessor();
        if (processor != null) processor.process(getClient(), command);
    }

}
