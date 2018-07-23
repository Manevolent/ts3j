package com.github.manevolent.ts3j;

import junit.framework.TestCase;

import java.net.InetSocketAddress;

public class ServerConnectionTest extends TestCase {
    public ServerConnectionTest(String name) {
        super(name);
    }

    public void testParser() throws Exception {
        Teamspeak3Client client = new Teamspeak3Client();

        client.connect(new InetSocketAddress(
                "voice.teamspeak.com",
                        9987),
                null,
                10000L
        );

        assertEquals(client.getClientConnectionState(), ClientConnectionState.CONNECTED);
    }
}
