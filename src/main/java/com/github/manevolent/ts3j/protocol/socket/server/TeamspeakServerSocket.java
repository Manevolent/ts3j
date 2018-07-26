package com.github.manevolent.ts3j.protocol.socket.server;

import com.github.manevolent.ts3j.protocol.socket.client.TeamspeakClientSocket;

public interface TeamspeakServerSocket {

    TeamspeakClientSocket accept();

}
