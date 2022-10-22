package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.fragment.Fragments;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketReassembly;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.QuickLZ;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FragmentTest extends TestCase {

    public static void main(String[] args) throws Exception {
        FragmentTest fragmentTest = new FragmentTest();

        fragmentTest.testParser();
        fragmentTest.testFragmentedFlag();
        fragmentTest.testNewProtocolFlag();
    }

    private List<Packet> getSplitPackets(boolean isNewProtocol) {
        ClientPacketHeader header = new ClientPacketHeader();
        header.setType(PacketBodyType.COMMAND);
        if (isNewProtocol) {
            header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
        }

        Packet largePacket = new Packet(ProtocolRole.CLIENT, header);

        largePacket.setBody(new PacketBody2Command(
                ProtocolRole.CLIENT,
                String.join("", Collections.nCopies(1000, "a!")) + "!"
        ));

        return Fragments.split(largePacket);
    }

    /**
     * All parts should have NEW_PROTOCOL flag if original Packet has it
     */
    public void testNewProtocolFlag() {
        for (Packet piece : getSplitPackets(false)) {
            assertFalse(
                    "Unexpected NEW_PROTOCOL flag",
                    piece.getHeader().getPacketFlag(HeaderFlag.NEW_PROTOCOL)
            );
        }

        for (Packet piece : getSplitPackets(true)) {
            assertTrue(
                    "NEW_PROTOCOL flag expected",
                    piece.getHeader().getPacketFlag(HeaderFlag.NEW_PROTOCOL)
            );
        }
    }

    /**
     * First and last fragmented packets should have FRAGMENTED flag.
     */
    public void testFragmentedFlag() {
        Ts3Debugging.setEnabled(true);
        ClientPacketHeader header = new ClientPacketHeader();

        header.setType(PacketBodyType.COMMAND);
        header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
        header.setPacketFlag(HeaderFlag.COMPRESSED, true);

        Packet largePacket = new Packet(ProtocolRole.CLIENT, header);

        /*
         * We need to construct a packet which should be split
         * to exactly two packets of MAXIMUM_PACKET_SIZE size after compression.
         */

        int targetCompressedBodyLength = 2 * (Fragments.MAXIMUM_PACKET_SIZE - largePacket.getHeader().getSize());


        //this may be fragile if MAXIMUM_PACKET_SIZE or split implementation will change
        String payload = String.join("", Collections.nCopies(482, "a!")) + "!";

        assertEquals(
                "Unexpected compressed body length",
                targetCompressedBodyLength,
                QuickLZ.compress(payload.getBytes(), 1).length
        );

        largePacket.setBody(new PacketBody2Command(ProtocolRole.CLIENT, payload));

        List<Packet> pieces = Fragments.split(largePacket);

        assertEquals(2, pieces.size());

        assertTrue(
                "First packet should have FRAGMENTED flag set.",
                pieces.get(0).getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)
        );

        assertTrue(
                "Last packet should have FRAGMENTED flag set.",
                pieces.get(1).getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)
        );
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
