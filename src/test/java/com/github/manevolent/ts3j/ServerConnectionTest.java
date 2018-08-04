package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.api.Channel;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

public class ServerConnectionTest  {
    public static void main(String[] args) throws Exception {
        try {
            Ts3Debugging.setEnabled(true);

            LocalIdentity identity = LocalIdentity.load(
                    new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU="))
            );

            identity.improveSecurity(10);

            LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

            client.setIdentity(identity);

            client.setNickname("Hello from Java");

            client.setOption("client.hwid", "JAVAJAVAJAVA");

            client.connect(new InetSocketAddress(
                            "ts.teamlixo.net",
                            9987),
                    null,
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            client.sendServerMessage("Heyyyy");

            while (true) {
                Thread.sleep(1000L);

                for (Channel channel : client.listChannels()) {
                    Ts3Debugging.debug(channel.getName());
                }

            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
