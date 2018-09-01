package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.TS3DNS;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

public class TS3DNSTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new TS3DNSTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        List<InetSocketAddress> soaddr = TS3DNS.lookup("voice.teamspeak.com");

        assertTrue(soaddr.size() > 0);

        for (SocketAddress socketAddress : soaddr)
            Ts3Debugging.debug(socketAddress);
    }
}
