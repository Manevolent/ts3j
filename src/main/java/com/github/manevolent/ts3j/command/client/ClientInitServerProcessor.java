package com.github.manevolent.ts3j.command.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.ComplexCommand;
import com.github.manevolent.ts3j.command.SimpleCommand;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

public class ClientInitServerProcessor implements CommandProcessor {
    @Override
    public void process(AbstractTeamspeakClientSocket socket, ComplexCommand complexCommand) throws CommandProcessException {
        SimpleCommand command = complexCommand.simplify();

        Ts3Debugging.debug(command.get("virtualserver_welcomemessage").getValue());

        if (socket.getState() == ClientConnectionState.RETRIEVING_DATA)
            try {
                socket.setState(ClientConnectionState.CONNECTED);
            } catch (Exception e) {
                throw new CommandProcessException(e);
            }
    }
}
