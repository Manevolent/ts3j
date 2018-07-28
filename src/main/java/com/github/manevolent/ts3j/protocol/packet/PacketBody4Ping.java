package com.github.manevolent.ts3j.protocol.packet;

import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;

import java.nio.ByteBuffer;

public class PacketBody4Ping extends PacketBody {
    public PacketBody4Ping(ProtocolRole role) {
        super(PacketBodyType.PING, role);
    }

    @Override
    public void setHeaderValues(PacketHeader header) {
        header.setPacketFlag(HeaderFlag.UNENCRYPTED, true);
    }

    @Override
    public void read(ByteBuffer buffer) {

    }

    @Override
    public void write(ByteBuffer buffer) {

    }

    @Override
    public int getSize() {
        return 0;
    }
}
