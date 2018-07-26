package com.github.manevolent.ts3j.protocol.packet.handler.local;

import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class LocalClientHandlerNegotiating extends LocalClientHandler {
    public LocalClientHandlerNegotiating(LocalTeamspeakClientSocket client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException, TimeoutException {

    }
}
