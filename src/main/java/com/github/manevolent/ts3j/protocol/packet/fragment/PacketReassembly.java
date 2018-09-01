package com.github.manevolent.ts3j.protocol.packet.fragment;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.socket.client.AbstractTeamspeakClientSocket;
import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.QuickLZ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class PacketReassembly {
    private final AbstractTeamspeakClientSocket.LocalCounter counter =
            new AbstractTeamspeakClientSocket.LocalCounterFull(65536, true);

    private final Map<Integer, Packet> queue = new LinkedHashMap<>();

    public void put(Packet packet) throws IOException {
        queue.put(packet.getHeader().getPacketId(), packet);
    }

    public Packet next() throws IOException {
        if (queue.size() <= 0) return null;

        List<Packet> reassemblyList = new ArrayList<>();

        try {
            // Pull out all other packets in the queue before this one which would also be fragmented
            List<Integer> packetIds = new ArrayList<>();

            boolean state = false;

            AbstractTeamspeakClientSocket.LocalCounter temporaryCounter =
                    new AbstractTeamspeakClientSocket.LocalCounterFull(65536, true);

            Pair<Integer, Integer> currentPacketId = counter.current();

            temporaryCounter.setPacketId(currentPacketId.getKey());
            temporaryCounter.setGeneration(currentPacketId.getValue());

            while (queue.size() > 0) {
                Pair<Integer, Integer> thisPacketId = temporaryCounter.current();
                Packet packet = queue.get(thisPacketId.getKey());

                if (packet == null)
                    break; // Chain breaks here
                else {
                    packetIds.add(thisPacketId.getKey());
                    reassemblyList.add(packet);

                    if (packet.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
                        state = !state;

                        if (!state) {
                            break; // fragmentation stopped, we found the last packet
                        }
                    } else if (!state) {
                        break; // don't reassemble this packet, it's not a fragment
                    } else if (state) {
                        // keep collecting fragments
                    }
                }

                temporaryCounter.next();
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

            // Count
            counter.next(packetIds.size());

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
        counter.reset();
        queue.clear();
    }
}