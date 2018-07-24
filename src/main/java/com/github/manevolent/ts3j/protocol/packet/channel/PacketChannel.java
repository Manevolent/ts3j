package com.github.manevolent.ts3j.protocol.packet.channel;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PacketChannel {
    private final Queue<NetworkPacket> receive = new ConcurrentLinkedQueue<>();
    private final ReassemblyQueue reassemblyQueue = new ReassemblyQueue();

    private PacketHandler handler;

    /**
     * Inserts a packet into the receive queue.
     * @param networkPacket Packet to insert.
     */
    public void insert(NetworkPacket networkPacket) {
        if ((networkPacket = reassemblyQueue.put(networkPacket)) != null)
            receive.add(networkPacket);
    }

    /**
     * Finds the next packet in the receive queue available for dequeue.
     * @return Network packet, null otherwise
     */
    public NetworkPacket next() {
        try {
            return receive.remove();
        } catch (NoSuchElementException ex) {
            return null;
        }
    }

    public PacketHandler getHandler() {
        return handler;
    }

    public void setHandler(PacketHandler handler) throws IOException {
        if (this.handler != handler) {
            if (this.handler != null) this.handler.onUnassigning();
            this.handler = handler;
            if (handler != null) handler.onAssigned();
        }
    }
}
