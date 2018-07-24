package com.github.manevolent.ts3j.protocol.server;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.RemoteEndpoint;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.client.LocalTeamspeakClient;
import com.github.manevolent.ts3j.protocol.packet.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;

public class RemoteTeamspeakServer extends RemoteEndpoint implements TeamspeakServer {
    private final LocalTeamspeakClient client;
    private final InetSocketAddress address;

    public RemoteTeamspeakServer(LocalTeamspeakClient client,
                                 InetSocketAddress address) {
        this.client = client;
        this.address = address;
    }

    @Override
    public SocketRole getRole() {
        return SocketRole.SERVER;
    }

    @Override
    public void send(Packet packet) throws IOException {
        client.send(packet);
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return address;
    }
}
