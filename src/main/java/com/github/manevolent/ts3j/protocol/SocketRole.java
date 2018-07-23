package com.github.manevolent.ts3j.protocol;

public enum SocketRole {
    CLIENT(ProtocolRole.SERVER, ProtocolRole.CLIENT),
    SERVER(ProtocolRole.CLIENT, ProtocolRole.SERVER);

    private final ProtocolRole in, out;

    SocketRole(ProtocolRole in, ProtocolRole out) {
        this.in = in;
        this.out = out;
    }

    public ProtocolRole getIn() {
        return in;
    }

    public ProtocolRole getOut() {
        return out;
    }
}
