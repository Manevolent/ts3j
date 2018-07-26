package com.github.manevolent.ts3j.protocol.socket;

import com.github.manevolent.ts3j.protocol.SocketRole;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface TeamspeakSocket extends Closeable {

    SocketRole getRole();

    InetSocketAddress getLocalSocketAddress();

}
