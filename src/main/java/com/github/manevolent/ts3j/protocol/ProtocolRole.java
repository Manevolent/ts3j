package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.header.ServerPacketHeader;

public enum ProtocolRole {
    CLIENT(ClientPacketHeader.class),
    SERVER(ServerPacketHeader.class);

    private final Class<? extends PacketHeader> headerClass;

    ProtocolRole(Class<? extends PacketHeader> headerClass) {
        this.headerClass = headerClass;
    }

    public Class<? extends PacketHeader> getHeaderClass() {
        return headerClass;
    }
}
