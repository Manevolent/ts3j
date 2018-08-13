package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.fragment.Fragments;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketReassembly;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.util.List;
import java.util.Random;

public class FragmentTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new FragmentTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        AbstractTeamspeakClientSocket.LocalCounterFull counter =
                new AbstractTeamspeakClientSocket.LocalCounterFull(65536, true);

        PacketReassembly reassembly = new PacketReassembly();

        for (int i = 0; i < 10_000; i ++) {
            byte[] data = new byte[5003];
            new Random(0x1234567).nextBytes(data);

            ClientPacketHeader header = new ClientPacketHeader();
            Packet largePacket = new Packet(ProtocolRole.CLIENT, header);
            header.setType(PacketBodyType.COMMAND);

            String text = Base64.toBase64String(data);
            largePacket.setBody(new PacketBody2Command(ProtocolRole.CLIENT, text));

            List<Packet> pieces = Fragments.split(largePacket);

            for (Packet piece : pieces) {
                Pair<Integer, Integer> packetId = counter.current();

                piece.getHeader().setPacketId(packetId.getKey());
                piece.getHeader().setGeneration(packetId.getValue());

                reassembly.put(piece);

                counter.next();
            }

            Packet reassembled;
            boolean read = false;
            while (((reassembled = reassembly.next()) != null)) {
                assertEquals(
                        ((PacketBody2Command) largePacket.getBody()).getText(),
                        ((PacketBody2Command) reassembled.getBody()).getText()
                );

                read = true;
            }

            assertEquals(true, read);
        }
    }

}
