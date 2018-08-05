package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.api.Client;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;
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

            client.setMicrophone(new Microphone() {
                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public CodecType getCodec() {
                    return CodecType.OPUS_MUSIC;
                }

                @Override
                public byte[] provide() {
                    System.err.println("Provide.");
                    return new byte[0];
                }
            });

            client.setIdentity(identity);

            client.setNickname("Hello from Java");

            client.setOption("client.hwid", "JAVAJAVAJAVA");

            client.connect(new InetSocketAddress(
                            "ts.teamlixo.net",
                            9987),
                    null,
                    10000L
            );

            client.sendServerMessage("\u26D4 You are not listening to this channel.");

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            client.setNickname("gjklsdfkjlgdsfgd2");

            client.waitForState(ClientConnectionState.DISCONNECTED, 10000L);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
