package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.util.Ts3Debugging;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeoutException;

public class LocalTeamspeakClientSocket extends AbstractTeamspeakClientSocket {
    private final DatagramSocket socket;

    private final DatagramPacket packet = new DatagramPacket(new byte[500], 500);

    {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public LocalTeamspeakClientSocket() {
        super();
    }

    @Override
    protected NetworkPacket readNetworkPacket() throws IOException {
        socket.receive(packet);

        Ts3Debugging.debug("[NETWORK] READ len=" + packet.getLength() + " from " + packet.getSocketAddress());

        PacketHeader header;

        try {
            header = getRole().getIn().getHeaderClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(packet.getData(), packet.getOffset(), packet.getLength())
                .order(ByteOrder.BIG_ENDIAN);

        header.read(buffer);

        return new NetworkPacket(
                packet,
                header,
                buffer
        );
    }

    @Override
    protected void writeNetworkPacket(NetworkPacket packet) throws IOException {
        Ts3Debugging.debug("[NETWORK] WRITE id=" + packet.getHeader().getPacketId() + " len=" +
                packet.getDatagram().getLength()
                + " to " +
                socket.getRemoteSocketAddress());
        Ts3Debugging.debug("[NETWORK] WRITE " + Ts3Debugging.getHex(packet.getBuffer().array()));

        socket.send(packet.getDatagram());
    }


    @Override
    public LocalIdentity getIdentity() {
        return (LocalIdentity) super.getIdentity();
    }

    public void setIdentity(LocalIdentity identity) {
        super.setIdentity(identity);
    }

    /**
     * Initiates a connection to a server
     * @param remote remote sever to contact
     * @param password server password
     * @param timeout timeout, in milliseconds, to complete a connection.
     * @throws IOException
     */
    public void connect(InetSocketAddress remote, String password, long timeout)
            throws IOException, TimeoutException {
        try {
            Ts3Debugging.debug("Connecting to " + remote + "...");

            ClientConnectionState connectionState = getState();

            if (connectionState != ClientConnectionState.DISCONNECTED)
                throw new IllegalStateException(connectionState.name());

            setClientId(0);
            setPacketId(0);
            setGenerationId(0);

            setOption("client.hostname", remote.getHostString());
            //setOption("client.password", password);

            socket.connect(remote);

            setState(ClientConnectionState.CONNECTING);

            waitForState(ClientConnectionState.CONNECTED, timeout);
        } catch (TimeoutException e) {
            setState(ClientConnectionState.DISCONNECTED);

            throw e;
        } catch (Throwable e) {
            setState(ClientConnectionState.DISCONNECTED);

            throw new IOException(e);
        }
    }

    @Override
    protected boolean isReading() {
        return socket.isConnected();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
