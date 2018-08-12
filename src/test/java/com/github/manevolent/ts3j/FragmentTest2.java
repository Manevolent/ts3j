package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.fragment.Fragments;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketBodyFragment;
import com.github.manevolent.ts3j.protocol.packet.fragment.PacketReassembly;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FragmentTest2 extends TestCase {

    public static void main(String[] args) throws Exception {
        new FragmentTest2().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        String[] packets = new String[] {
            // Put diagnostic here
        };

        List<Packet> pieces = new ArrayList<>();

        for (int i = 0; i < packets.length; i ++) {
            Packet packet = new Packet(ProtocolRole.SERVER);
            ByteBuffer buffer = ByteBuffer.wrap(Base64.decode(packets[i]));
            packet.readHeader(buffer);
            packet.setBody(new PacketBodyFragment(packet.getHeader().getType(), ProtocolRole.SERVER));
            packet.readBody(buffer);
            pieces.add(packet);

            Ts3Debugging.debug(
                    "Read type=" + packet.getHeader().getType()
                    + " id=" + packet.getHeader().getPacketId()
                    + " flags=" + packet.getHeader().getPacketFlags()
                    + " fragment=" + packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)
            );
        }

        Ts3Debugging.debug("Loaded " + pieces.size() + " pieces.");

        PacketReassembly reassembly = new PacketReassembly();
        for (Packet piece : pieces) {
            Packet out = reassembly.put(piece);
            if (out != null) {
                Ts3Debugging.debug(((PacketBody2Command)out.getBody()).getText());
            }
        }
    }

}
