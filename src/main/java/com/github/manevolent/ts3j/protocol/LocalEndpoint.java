package com.github.manevolent.ts3j.protocol;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.socket.LocalTeamspeakSocket;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public abstract class LocalEndpoint implements Endpoint {
    private final LocalTeamspeakSocket socket;


    private PacketHandler handler = null;
    private LocalIdentity localIdentity = null;

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

    public void setHandler(PacketHandler handler) throws IOException {
        if (this.handler != handler) {
            if (this.handler != null) this.handler.onUnassigning();
            this.handler = handler;
            if (handler != null) handler.onAssigned();
        }
    }

    protected void handlePacket(NetworkPacket packet) throws IOException {
        if (this.handler != null) this.handler.handlePacket(packet);
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
                    handlePacket(receive());
                } catch (Exception e) {
                    if (e instanceof InterruptedException) continue;

                    Ts3Logging.debug("Problem handling packet", e);
                }
            }

            Ts3Logging.debug("Leaving packet receiver");
        }
    }
}
