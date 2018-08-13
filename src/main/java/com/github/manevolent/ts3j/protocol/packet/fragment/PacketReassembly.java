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
    private int currentPacketId = 0;

    public void setPacketId(int currentPacketId) {
        this.currentPacketId = currentPacketId;
    }

    public boolean put(Packet packet) throws IOException {
        queue.put(packet.getHeader().getPacketId(), packet);

        return currentPacketId == packet.getHeader().getPacketId();
    }

    public Packet next() throws IOException {
        List<Packet> reassemblyList = new ArrayList<>();

        try {
            // Pull out all other packets in the queue before this one which would also be fragmented
            List<Integer> packetIds = new ArrayList<>();

            boolean state = false;

            for (int packetId = currentPacketId; ; packetId++) {
                if (packetId >= 65536) packetId = 0;

                Packet packet = queue.get(packetId);

                if (packet == null)
                    break; // Chain breaks here
                else {
                    packetIds.add(packetId);
                    reassemblyList.add(packet);

                    if (packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
                        state = !state;

                        if (!state) {
                            currentPacketId += packetIds.size();
                            break; // fragmentation stopped, we found the last packet
                        } else
                            continue; // keep collecting fragments
                    } else if (!state) {
                        currentPacketId ++;
                        break; // don't reassemble this packet, it's not a fragment
                    } else if (state) {
                        // keep collecting fragments
                        continue;
                    }
                }
            }

            if (state) return null; // cannot reassemble yet because we don't have all fragments
            else if (reassemblyList.size() <= 0) return null; // cannot reassemble because we miss the first packet

            // Rebuild a master packet from the contents of all previous packet fragments
            int totalLength =
                    reassemblyList
                            .stream()
                            .mapToInt(x -> x.getBody().getSize())
                            .sum();

            if (totalLength < 0) throw new IllegalArgumentException("reassembly too small: " + totalLength);

            Packet firstPacket = reassemblyList.get(0);
            Packet reassembledPacket = new Packet(firstPacket.getRole());
            reassembledPacket.setHeader(firstPacket.getHeader().clone());

            ByteBuffer reassemblyBuffer = ByteBuffer.allocate(totalLength);

            reassembledPacket.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, false);

            for (Packet old : reassemblyList)
                old.writeBody(reassemblyBuffer);

            reassemblyBuffer.position(0);

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

    public void reset() {
        currentPacketId = 0;
        queue.clear();
    }
}