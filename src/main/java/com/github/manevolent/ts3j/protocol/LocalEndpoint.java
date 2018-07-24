package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.PacketType;
import com.github.manevolent.ts3j.protocol.packet.channel.PacketChannel;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.socket.LocalTeamspeakSocket;
import com.github.manevolent.ts3j.protocol.packet.channel.ReassemblyQueue;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public abstract class LocalEndpoint implements Endpoint {
    private final LocalTeamspeakSocket socket;

    private LocalIdentity localIdentity = null;

    private final ReassemblyQueue reassemblyQueue = new ReassemblyQueue();

    private final Object runLock = new Object();
    private Thread receiverThread;
    private boolean running = false;

    protected LocalEndpoint(LocalTeamspeakSocket socket) {
        this.socket = socket;
    }

    protected LocalEndpoint(SocketRole socketRole) throws SocketException {
        this.socket = new LocalTeamspeakSocket(socketRole);
    }

    public InetSocketAddress getLocalAddress() {
        return getSocket().getLocalAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return getSocket().getLocalAddress();
    }

    protected final LocalTeamspeakSocket getSocket() {
        return socket;
    }

    protected abstract void acknowledge(NetworkPacket packet, PacketType ackType) throws IOException;
    protected abstract PacketChannel getChannel(NetworkPacket packet);

    public void setRunning(boolean running) {
        if (this.running != running) {
            this.running = running;

            if (running) {
                Ts3Logging.debug("Starting network receiver thread...");

                receiverThread = new Thread(new PacketReceiver());
                receiverThread.setDaemon(true);
                receiverThread.start();
            } else {
                if (receiverThread != null) {
                    receiverThread.interrupt();
                    receiverThread = null;
                }
            }
        }
    }

    @Override
    public SocketRole getRole() {
        return socket.getSocketRole();
    }

    private NetworkPacket receive() throws IOException {
        return socket.receive();
    }

    public LocalIdentity getLocalIdentity() {
        return localIdentity;
    }

    public void setLocalIdentity(LocalIdentity localIdentity) {
        this.localIdentity = localIdentity;
    }

    private class PacketReceiver implements Runnable {
        @Override
        public void run() {
            Ts3Logging.debug("Entering packet receiver");

            while (running) {
                try {
                    NetworkPacket packet = receive();
                    PacketChannel channel = getChannel(packet);
                    if (channel == null) continue;

                    channel.insert(packet);

                    if (packet.getHeader().getType().getAcknolwedgedBy() != null)
                        acknowledge(packet, packet.getHeader().getType().getAcknolwedgedBy());

                    PacketHandler handler = channel.getHandler();
                    if (handler != null) {
                        while ((packet = channel.next()) != null) {
                            handler.handlePacket(packet);
                        }
                    }
                } catch (Exception e) {
                    if (e instanceof InterruptedException) continue;

                    Ts3Logging.debug("Problem handling packet", e);
                }
            }

            Ts3Logging.debug("Leaving packet receiver");
        }
    }
}
