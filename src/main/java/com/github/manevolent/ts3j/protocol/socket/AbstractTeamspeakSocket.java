package com.github.manevolent.ts3j.protocol.socket;

import com.github.manevolent.ts3j.protocol.SocketRole;

public abstract class AbstractTeamspeakSocket implements TeamspeakSocket {
    private final SocketRole role;

    protected AbstractTeamspeakSocket(SocketRole role) {
        this.role = role;
    }

    public SocketRole getRole() {
        return role;
    }
}
