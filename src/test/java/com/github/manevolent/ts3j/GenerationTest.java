package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.nio.ByteBuffer;

public class GenerationTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new GenerationTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        byte[] fakeMac = new byte[8], iv = new byte[16];
        PacketTransformation transformation = new PacketTransformation(iv, fakeMac);

        AbstractTeamspeakClientSocket.LocalCounterFull local =
                new AbstractTeamspeakClientSocket.LocalCounterFull(65536, true);

        AbstractTeamspeakClientSocket.RemoteCounterFull remote =
                new AbstractTeamspeakClientSocket.RemoteCounterFull(65536, 100);

        ClientPacketHeader header = new ClientPacketHeader();

        for (int i = 0; i < 100_000; i ++) {
            // CLIENT side
            Pair<Integer, Integer> pair = local.next();

            header.setType(PacketBodyType.COMMAND);
            header.setPacketId(pair.getKey());
            header.setGeneration(pair.getValue());

            Packet packet = new Packet(ProtocolRole.CLIENT, header);
            packet.setBody(new PacketBody2Command(ProtocolRole.CLIENT, "Hello world " + i + "!"));

            ByteBuffer sendBuffer = transformation.encrypt(packet);

            // SERVER side
            header.setGeneration(remote.getGeneration(header.getPacketId()));
            transformation.decrypt(header, sendBuffer, packet.getBody().getSize());

            remote.put(header.getPacketId());
        }
    }

}
