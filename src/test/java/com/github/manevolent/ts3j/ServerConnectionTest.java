package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.command.SingleCommand;
import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
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
                    return new byte[20];
                }
            });

            assertEquals(client.getState(), ClientConnectionState.CONNECTED);

            while (true) {
                Thread.sleep(1000L);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
