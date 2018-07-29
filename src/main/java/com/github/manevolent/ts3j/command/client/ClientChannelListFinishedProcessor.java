package com.github.manevolent.ts3j.command.client;

import com.github.manevolent.ts3j.command.CommandProcessException;
import com.github.manevolent.ts3j.command.CommandProcessor;
import com.github.manevolent.ts3j.command.MultiCommand;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;

public class ClientChannelListFinishedProcessor implements CommandProcessor {
    @Override
    public void process(AbstractTeamspeakClientSocket client,
                        MultiCommand command) throws CommandProcessException {
        if (client.getState() == ClientConnectionState.RETRIEVING_DATA)
            try {
                client.setState(ClientConnectionState.CONNECTED);
            } catch (Exception e) {
                throw new CommandProcessException(e);
            }
    }
}
