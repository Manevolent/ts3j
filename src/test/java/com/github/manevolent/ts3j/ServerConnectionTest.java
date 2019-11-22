package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.*;
import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class ServerConnectionTest  {
    public static void main(String[] args) throws Exception {
        Ts3Debugging.setEnabled(true);

        LocalIdentity identity = LocalIdentity.generateNew(10);

        LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();
        client.setIdentity(identity);
        client.setNickname(ServerConnectionTest.class.getSimpleName());
        client.setHWID("TestTestTest");

        try {
            client.connect(
                    "teamlixo.net",
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            Thread.sleep(1000);

            client.setDescription("Heyo");

            client.subscribeAll();

            Thread.sleep(100000);

            client.disconnect("BYE");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Thread.sleep(3000L);
    }
}
