package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Base64;

public class ServerConnectionTest extends TestCase {
    public ServerConnectionTest() {
        super("ServerConnectionTest");
    }

    public void testParser() throws Exception {
        try {
            LocalIdentity localIdentity = LocalIdentity.load(
                    Ts3Crypt.decodePublicKey(Base64.getDecoder()
                            .decode("MEsDAgcAAgEgAiBpPRbTliVt9KxtIz8saYdwcnNgcwaKLbKYSpDNO87u9gIgSWWPKcSJ9P6VZKJfRdpWwcfMdJv+NA9/hXUtz1uwRVI=")),
                    new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU=")),
                    2294,
                    2295
            );

            LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

            client.setIdentity(localIdentity);

            client.connect(new InetSocketAddress(
                            "voice.teamspeak.com",
                            9987),
                    null,
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new ServerConnectionTest().testParser();
    }
}
