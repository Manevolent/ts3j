package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import junit.framework.TestCase;

import java.net.InetSocketAddress;

public class ServerConnectionTest extends TestCase {
    public ServerConnectionTest(String name) {
        super(name);
    }

    public void testParser() throws Exception {
        try {
            LocalIdentity localIdentity = LocalIdentity.generateNew(8);

            LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

            client.setIdentity(localIdentity);

            client.connect(new InetSocketAddress(
                            "ts.teamlixo.net",
                            9987),
                    null,
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
