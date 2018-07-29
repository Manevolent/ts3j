package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.model.Channel;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ServerConnectionTest  {
    public static void main(String[] args) throws Exception {
        try {
            LocalIdentity localIdentity = LocalIdentity.generateNew(10);

            LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

            client.setIdentity(localIdentity);

            client.setNickname("Hello from Java");

            client.setOption("client.hwid", "JAVAJAVAJAVA");

            client.connect(new InetSocketAddress(
                            "ts.teamlixo.net",
                            9987),
                    null,
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            Ts3Debugging.info("Connected to " + client.getVirtualServer().getName() + ".");
            Ts3Debugging.info(" Client name: " + client.getSelf().getNickname());
            Ts3Debugging.info(" Channels: " +
                    client.getVirtualServer().getChannelsOrderd()
                    .stream().map(Channel::getName).collect(Collectors.toList())
            );
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
