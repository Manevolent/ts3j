package com.github.manevolent.ts3j.protocol.packet;

public enum PacketBodyType {
    VOICE(0x0, null, null, false, false, false, false, false),
    VOICE_WHISPER(0x1, null, null, false, false, false, false, false),
    PONG(0x5, null, null, false, false, false, false, false),
    PING(0x4, null, PONG, false, false, false, false, false),
    ACK(0x6, PacketBody6Ack.class, null, true, true, false, false, true),
    ACK_LOW(0x7, null, null, true, true, false, false, true),
    INIT1(0x8, PacketBody8Init1.class, null, false, true, false, false, false),
    COMMAND_LOW(0x3, null, ACK_LOW, true, true, true, true, true),
    COMMAND(0x2, PacketBody2Command.class, ACK, true, true, true, true, true);

    private final int index;
    private final Class<? extends PacketBody> packetClass;

    private final PacketBodyType acknolwedgedBy;
    private final boolean encrypted, resend, splittable, compressible, mustEncrypt;

    PacketBodyType(int index, Class<? extends PacketBody> packetClass,
                   PacketBodyType acknolwedgedBy,
                   boolean encrypted,
                   boolean resend,
                   boolean splittable,
                   boolean compressible, boolean mustEncrypt) {
        this.index = index;
        this.packetClass = packetClass;
        this.acknolwedgedBy = acknolwedgedBy;
        this.resend = resend;
        this.encrypted = encrypted;
        this.splittable = splittable;
        this.compressible = compressible;
        this.mustEncrypt = mustEncrypt;
    }

    public int getIndex() {
        return index;
    }

    public static PacketBodyType fromId(int index) {
        for (PacketBodyType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }

    public Class<? extends PacketBody> getPacketClass() {
        return packetClass;
    }

    public boolean canEncrypt() {
        return encrypted;
    }

    public PacketBodyType getAcknolwedgedBy() {
        return acknolwedgedBy;
    }

    public boolean canResend() {
        return resend;
    }

    public boolean isSplittable() {
        return splittable;
    }

    public boolean isCompressible() {
        return compressible;
    }

    public boolean mustEncrypt() {
        return mustEncrypt;
    }
}
