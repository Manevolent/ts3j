package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.enums.ConnectionState;
import junit.framework.TestCase;

import java.net.InetSocketAddress;

public class ServerConnectionTest extends TestCase {
    public ServerConnectionTest(String name) {
        super(name);
    }

    public void testParser() throws Exception {
        TeamspeakClient client = new TeamspeakClient();

        client.connect(new InetSocketAddress(
                "voice.teamspeak.com",
                        9987),
                null,
                10000L
        );

        assertEquals(client.getConnectionState(), ConnectionState.CONNECTED);
    }
}
