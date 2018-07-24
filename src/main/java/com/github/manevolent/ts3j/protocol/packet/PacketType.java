package com.github.manevolent.ts3j.protocol.packet;

public enum PacketType {
    VOICE(0x0, null, null, false, false, false, false),
    VOICE_WHISPER(0x1, null, null, false, false, false, false),
    PONG(0x5, null, null, false, false, false, false),
    PING(0x4, null, PONG, false, false, false, false),
    ACK(0x6, null, null, true, true, false, false),
    ACK_LOW(0x7, null, null, true, true, false, false),
    INIT1(0x8, Packet8Init1.class, null, false, true, false, false),
    COMMAND_LOW(0x3, null, ACK_LOW, true, true, true, true),
    COMMAND(0x2, Packet2Command.class, ACK, true, true, true, true)
    ;

    private final int index;
    private final Class<? extends Packet> packetClass;

    private final PacketType acknolwedgedBy;
    private final boolean encrypted, resend, splittable, compressible;

    PacketType(int index, Class<? extends Packet> packetClass,
               PacketType acknolwedgedBy,
               boolean encrypted,
               boolean resend,
               boolean splittable,
               boolean compressible) {
        this.index = index;
        this.packetClass = packetClass;
        this.acknolwedgedBy = acknolwedgedBy;
        this.resend = resend;
        this.encrypted = encrypted;
        this.splittable = splittable;
        this.compressible = compressible;
    }

    public int getIndex() {
        return index;
    }

    public static PacketType fromId(int index) {
        for (PacketType value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }

    public Class<? extends Packet> getPacketClass() {
        return packetClass;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public PacketType getAcknolwedgedBy() {
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
}
