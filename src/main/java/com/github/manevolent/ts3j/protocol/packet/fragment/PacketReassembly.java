package com.github.manevolent.ts3j.protocol.packet.fragment;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.util.QuickLZ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class PacketReassembly {
    private final Map<Integer, Packet> queue = new LinkedHashMap<>();
    private boolean state = false;

    public Packet reassemble(Packet lastFragment) throws IOException {
        List<Packet> reassemblyList = new ArrayList<>();

        try {
            // Pull out all other packets in the queue before this one which would also be fragmented
            List<Integer> packetIds = new ArrayList<>();

            for (int packetId = lastFragment.getHeader().getPacketId(); ; packetId--) {
                if (packetId < 0) packetId = 65536 + packetId;

                Packet olderPacket = queue.get(packetId);

                if (olderPacket == null)
                    break; // Chain breaks here
                else if (olderPacket.getHeader().getType() != lastFragment.getHeader().getType())
                    continue; // skip???
                else {
                    packetIds.add(packetId);
                    reassemblyList.add(olderPacket);

                    if (lastFragment.getHeader().getPacketId() != packetId &&
                            olderPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED))
                        break;
                }
            }

            // Reverse the collection
            Collections.reverse(reassemblyList);

            // Rebuild a master packet from the contents of all previous packet fragments
            int totalLength =
                    reassemblyList
                            .stream()
                            .mapToInt(x -> x.getBody().getSize())
                            .sum();

            if (totalLength < 0) throw new IllegalArgumentException("reassembly too small: " + totalLength);

            Packet firstPacket = reassemblyList.get(0);
            Packet reassembledPacket = new Packet(firstPacket.getRole());
            reassembledPacket.setHeader(firstPacket.getHeader());

            ByteBuffer reassemblyBuffer = ByteBuffer.allocate(totalLength);

            reassembledPacket.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, false);

            for (Packet old : reassemblyList)
                old.writeBody(reassemblyBuffer);

            if (firstPacket.getHeader().getPacketFlag(HeaderFlag.COMPRESSED))
                reassemblyBuffer = ByteBuffer.wrap(QuickLZ.decompress(reassemblyBuffer.array()));

            reassembledPacket.readBody(reassemblyBuffer);

            // Remove read packets
            packetIds.forEach(queue::remove);

            return reassembledPacket;
        } catch (Exception ex) {
            throw new IOException("Problem reassembling " + reassemblyList.size() + " packet(s): [" +
                    String.join(",",
                            reassemblyList.stream().map(x -> "{" +
                                    Base64.getEncoder().encodeToString(x.toByteArray())
                                    + "}").collect(Collectors.toList())) + "]",
                    ex);
        }
    }

    public Packet put(Packet packet) throws IOException {
        Packet reassembled;

        if (packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
            boolean oldState = state;

            state = !state;

            if (!(packet.getBody() instanceof PacketBodyFragment))
                throw new IllegalArgumentException("packet fragment object is not representative of a fragment");

            queue.put(packet.getHeader().getPacketId(), packet);

            if (oldState) {
                reassembled = reassemble(packet);
            } else {
                reassembled = null;
            }
        } else {
            if (state) {
                queue.put(packet.getHeader().getPacketId(), packet);

                reassembled = null;
            } else {
                reassembled = packet;
            }
        }

        return reassembled;
    }

    public boolean isReassembling() {
        return state;
    }

    public void reset() {
        state = false;
        queue.clear();
    }
}