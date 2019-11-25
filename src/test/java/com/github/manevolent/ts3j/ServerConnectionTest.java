package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.audio.*;
import com.github.manevolent.ts3j.command.*;
import com.github.manevolent.ts3j.enums.*;
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
        client.setMicrophone(new Microphone() {
            @Override public boolean isReady() {
                return false;
            }

            @Override public CodecType getCodec() {
                return null;
            }

            @Override public byte[] provide() {
                return new byte[0];
            }
        });

        try {
            client.connect(
                    "teamlixo.net",
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            client.disconnect("BYE");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            client.close();
        }
    }
}
