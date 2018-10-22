package com.github.manevolent.ts3j;
;
import com.github.manevolent.ts3j.api.Client;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.event.*;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice;
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class ServerConnectionTest  {
    public static void main(String[] args) throws Exception {

        LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

        Ts3Debugging.setEnabled(false);

        LocalIdentity identity = LocalIdentity.generateNew(10);

        client.setIdentity(identity);
        client.setNickname("ts3j issue 2");
        client.setHWID("TestTestTest");

        client.setVoiceHandler(packet ->
                System.out.println(packet.getServerFlag0())
        );

        while (true) {
            try {
                client.connect(
                        "teamlixo.net",
                        10000L
                );

                assertEquals(client.getState(), ClientConnectionState.CONNECTED);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Thread.sleep(30000);
        }
    }
}
