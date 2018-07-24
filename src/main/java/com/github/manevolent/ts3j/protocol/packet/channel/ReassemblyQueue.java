package com.github.manevolent.ts3j.protocol.packet.channel;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.PacketFragment;
import com.github.manevolent.ts3j.protocol.packet.PacketType;

import java.nio.ByteBuffer;
import java.util.*;

public class ReassemblyQueue {
    private final Map<Integer, NetworkPacket> queue = new LinkedHashMap<>();
    private final Map<PacketType, Boolean> fragmentation = new LinkedHashMap<>();

    public NetworkPacket reassemble(NetworkPacket lastFragment) {
        // Pull out all other packets in the queue before this one which would also be fragmented
        List<NetworkPacket> reassemblyList = new ArrayList<>();
        for (int packetId = lastFragment.getHeader().getPacketId();; packetId--) {
            NetworkPacket olderPacket = queue.get(packetId);

            if (olderPacket == null)
                break; // Chain breaks here
            else if (olderPacket.getHeader().getType() != lastFragment.getHeader().getType())
                continue; // skip
            else {
                reassemblyList.add(olderPacket);
                fragmentation.remove(packetId); // Don't reuse this packet
            }
        }

        // Reverse the collection
        Collections.reverse(reassemblyList);

        // Rebuild a master packet from the contents of all previous packet fragments
        int totalLength = reassemblyList.stream().mapToInt(x -> x.getPacket().getSize()).sum() +
                lastFragment.getPacket().getSize();

        if (totalLength <= 0) throw new IllegalArgumentException("reassembly too small: " + totalLength);

        ByteBuffer reassemblyBuffer = ByteBuffer.allocate(totalLength);

        for (NetworkPacket old : reassemblyList)
            old.writeBody(reassemblyBuffer);

        NetworkPacket firstPacket = reassemblyList.get(0);
        NetworkPacket reassembledPacket = new NetworkPacket(firstPacket.getRole());
        reassembledPacket.setHeader(firstPacket.getHeader());
        reassembledPacket.getHeader().setPacketFlag(HeaderFlag.FRAGMENTED, false);
        reassembledPacket.readBody((ByteBuffer) reassemblyBuffer.position(0));

        return reassembledPacket;
    }

    public NetworkPacket put(NetworkPacket networkPacket) {
        Boolean fragmentState = fragmentation.get(networkPacket.getHeader().getType());

        if (networkPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED)) {
            if (!(networkPacket.getPacket() instanceof PacketFragment))
                throw new IllegalArgumentException("packet fragment object is not instance of PacketFragment class");

            boolean fragmentationEnded = false;

            if (fragmentState == null)
                fragmentation.put(networkPacket.getHeader().getType(), fragmentState = true);
            else if (fragmentState) {
                // Fragmentation end
                fragmentationEnded = true;

                fragmentation.put(networkPacket.getHeader().getType(), fragmentState = false);
            }

            queue.put(networkPacket.getHeader().getPacketId(), networkPacket);

            if (fragmentationEnded)
                return reassemble(networkPacket);

            return null;
        } else {
            if (fragmentState != null && fragmentState) {
                queue.put(networkPacket.getHeader().getPacketId(), networkPacket);

                return null;
            } else {
                return networkPacket;
            }
        }
    }
}
