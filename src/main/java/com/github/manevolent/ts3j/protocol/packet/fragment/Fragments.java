package com.github.manevolent.ts3j.protocol.packet.fragment;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyCompressed;
import com.github.manevolent.ts3j.util.QuickLZ;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Packet fragmentation helper.  Pulled out of the client logic for use in FragmentTest so we can track the stability
 * of fragmentation.
 */
public final class Fragments {
    /**
     * Maximum segment size (header + body) allowed to leave the split method
     */
    public static final int MAXIMUM_PACKET_SIZE = 500;

    /**
     * Splits a packet into several sub-packets.
     * @param packet Packet to send.
     * @return List of packets to send
     */
    public static List<Packet> split(Packet packet) {
        if (packet.getSize() > MAXIMUM_PACKET_SIZE) { // Check if packet is too large to send

            int size = packet.getBody().getSize();

            ByteBuffer outputBuffer = ByteBuffer.allocate(size);
            packet.getBody().write(outputBuffer);

            byte[] outputArray;

            if (packet.getHeader().getType().isCompressible()) {
                byte[] compressed = QuickLZ.compress(outputBuffer.array(), 1);

                Packet compressedPacket = new Packet(packet.getRole());
                compressedPacket.setHeader(packet.getHeader().clone());
                compressedPacket.getHeader().setPacketFlag(HeaderFlag.COMPRESSED, true);
                compressedPacket.setBody(
                        new PacketBodyCompressed(
                                packet.getHeader().getType(), packet.getRole(),
                                compressed
                        )
                );

                packet = compressedPacket;

                if (packet.getSize() <= MAXIMUM_PACKET_SIZE)
                    return Collections.singletonList(packet);

                outputArray = compressed;
                size = compressed.length;
            } else
                outputArray = outputBuffer.array();

            List<Packet> pieces = new LinkedList<>();

            int maxFragmentSize = MAXIMUM_PACKET_SIZE - packet.getHeader().getSize();

            for (int offs = 0; offs < size;) {
                int flush = Math.min(maxFragmentSize, size - offs);

                boolean first = offs == 0;
                boolean last = flush < maxFragmentSize;

                Packet piece = new Packet(packet.getRole());
                piece.setHeader(packet.getHeader().clone());

                if (!first) // Only first packet has flags
                    piece.getHeader().setPacketFlags(HeaderFlag.NONE.getIndex());

                // First and last packet get FRAGMENTED flag
                piece.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, first || last);

                byte[] pieceBytes = new byte[flush];
                System.arraycopy(outputArray, offs, pieceBytes, 0, flush);

                piece.setBody(new PacketBodyFragment(
                        packet.getHeader().getType(),
                        packet.getHeader().getRole(),
                        pieceBytes
                ));

                pieces.add(piece);

                offs += flush;
            }

            return pieces;
        } else {
            // No splitting or compression
            return Collections.singletonList(packet);
        }
    }

}
