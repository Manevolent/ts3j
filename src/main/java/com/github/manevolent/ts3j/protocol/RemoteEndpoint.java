package com.github.manevolent.ts3j.protocol;

import java.net.InetSocketAddress;

public abstract class RemoteEndpoint implements Endpoint {
    public abstract InetSocketAddress getRemoteAddress();
}
