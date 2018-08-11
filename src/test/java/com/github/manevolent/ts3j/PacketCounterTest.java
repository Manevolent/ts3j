package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import com.github.manevolent.ts3j.util.Pair;
import junit.framework.TestCase;

public class PacketCounterTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new PacketCounterTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        AbstractTeamspeakClientSocket.LocalCounter localCounter =
                new AbstractTeamspeakClientSocket.LocalCounterFull(65536, 0, true);

        AbstractTeamspeakClientSocket.RemoteCounter remoteCounter =
                new AbstractTeamspeakClientSocket.RemoteCounterFull(65536, 100);

        for (int i = 0; i < 65580 / 5; i ++) {
            Pair<Integer, Integer> packet = null;
            for (int n = 0; n < 5; n ++)
                packet = localCounter.next();

            assertEquals(true, remoteCounter.put(packet.getKey()));
            assertEquals(packet.getValue().intValue(), remoteCounter.getCurrentGeneration());
        }
    }

}