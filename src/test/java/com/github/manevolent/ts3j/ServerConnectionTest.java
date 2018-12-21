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
        Ts3Debugging.setEnabled(true);

        LocalTeamspeakClientSocket client = new LocalTeamspeakClientSocket();

        LocalIdentity identity = LocalIdentity.generateNew(10);

        client.setIdentity(identity);
        client.setNickname(ServerConnectionTest.class.getSimpleName());
        client.setHWID("TestTestTest");

        try {
            client.connect(
                    "teamlixo.net",
                    10000L
            );

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            client.disconnect("BYE");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Thread.sleep(3000L);
    }
}
