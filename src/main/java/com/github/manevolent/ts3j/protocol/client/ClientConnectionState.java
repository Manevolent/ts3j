
package com.github.manevolent.ts3j.protocol.client;

public enum ClientConnectionState {
    CONNECTING(),
    RETRIEVING_DATA(),
    CONNECTED(),
    DISCONNECTED();

    ClientConnectionState() {}
}