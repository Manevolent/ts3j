package com.github.manevolent.ts3j.protocol.packet.handler;

import com.github.manevolent.ts3j.command.Command;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.TeamspeakClientSocket;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public abstract class PacketHandler {
    private final TeamspeakClientSocket clientSocket;

    protected PacketHandler(TeamspeakClientSocket clientSocket) {
        this.clientSocket = clientSocket;
    }


    public void handleConnectionStateChanging(ClientConnectionState clientConnectionState) {

    }

    public void onUnassigning() throws IOException {

    }


    public void onAssigned() throws IOException, TimeoutException {

    }

    public void handlePacket(Packet packet) throws IOException, TimeoutException {

    }

    public TeamspeakClientSocket getClient() {
        return clientSocket;
    }
}
