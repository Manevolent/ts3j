package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.enums.CodecType;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.packet.PacketBody0Voice;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.nio.ByteBuffer;

/**
 * I just sent a series of voice packets and caught the last packet, turns out Teamspeak sends a 0-length byte
 * array on the last packet when the decoder should close.
 */
public class VoicePacketTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new VoicePacketTest().testParser();
    }

    public void testParser() throws Exception {
        byte[] packetBytes = Ts3Debugging.hexStringToByteArray("c1a81dfa4699030e0cc10002800cc205");
        Packet voicePacket = new Packet(ProtocolRole.CLIENT);
        voicePacket.read(ByteBuffer.wrap(packetBytes));

        assertEquals(PacketBodyType.VOICE, voicePacket.getHeader().getType());
        assertEquals(CodecType.OPUS_MUSIC, ((PacketBody0Voice) voicePacket.getBody()).getCodecType());
        assertEquals(0, ((PacketBody0Voice)voicePacket.getBody()).getClientId());
        assertEquals(3266, ((PacketBody0Voice)voicePacket.getBody()).getPacketId());
        assertEquals(0, ((PacketBody0Voice)voicePacket.getBody()).getCodecData().length);
    }

}
