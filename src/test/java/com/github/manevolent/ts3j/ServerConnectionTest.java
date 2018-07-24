package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.client.LocalTeamspeakClient;
import junit.framework.TestCase;

import java.net.InetSocketAddress;

public class ServerConnectionTest extends TestCase {
    public ServerConnectionTest(String name) {
        super(name);
    }

    public void testParser() throws Exception {
        try {
            LocalIdentity localIdentity = LocalIdentity.generateNew(8);

            LocalTeamspeakClient client = new LocalTeamspeakClient();

            client.setLocalIdentity(localIdentity);

            client.connect(new InetSocketAddress(
                            "voice.teamspeak.com",
                            9987),
                    null,
                    10000L
            );

            assertEquals(client.getConnectionState(), ClientConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
