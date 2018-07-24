package com.github.manevolent.ts3j.protocol.socket;

import com.github.manevolent.ts3j.protocol.packet.transformation.DefaultPacketTransformation;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;

public abstract class AbstractTeamspeakSocket implements TeamspeakSocket {
    private PacketTransformation transformation = new DefaultPacketTransformation();

    @Override
    public PacketTransformation getPacketTransformation() {
        return transformation;
    }

    @Override
    public void setPacketTransformation(PacketTransformation transformation) {
        this.transformation = transformation;
    }
}
