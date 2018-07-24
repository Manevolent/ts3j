package com.github.manevolent.ts3j.protocol.socket;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.SocketRole;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.protocol.packet.Packet;
import com.github.manevolent.ts3j.protocol.packet.PacketFragment;
import com.github.manevolent.ts3j.protocol.packet.PacketType;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Low-level protocol socket (not a client, used for server implementation also)
 *
 * Used simply to transcribe packets on the network
 */
public class LocalTeamspeakSocket extends AbstractTeamspeakSocket implements TeamspeakSocket {
    private final SocketRole socketRole;

    private final DatagramSocket datagramSocket;
    private final DatagramPacket datagramPacket = new DatagramPacket(new byte[1500], 1500);

    public LocalTeamspeakSocket(SocketRole socketRole, DatagramSocket datagramSocket) {
        this.socketRole = socketRole;
        this.datagramSocket = datagramSocket;
    }

    public LocalTeamspeakSocket(SocketRole socketRole) throws SocketException {
        this(socketRole, new DatagramSocket());
    }

    public SocketRole getSocketRole() {
        return socketRole;
    }

    public InetSocketAddress getLocalAddress() {
        return new InetSocketAddress(datagramSocket.getLocalAddress(), datagramSocket.getLocalPort());
    }

    public InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(datagramSocket.getInetAddress(), datagramSocket.getPort());
    }

    public void connect(SocketAddress remote) throws SocketException {
        datagramSocket.connect(remote);
    }

    /**
     * Writes an actual datagram to the network
     * @param packet Packet to write
     * @throws IOException
     */
    private void sendNetworkPacket(DatagramPacket packet) throws IOException {
        if (packet.getAddress() == null || packet.getPort() < 0)
            packet.setSocketAddress(datagramSocket.getRemoteSocketAddress());

        Ts3Logging.debug(
                "[NETWORK] WRITE Len=" + packet.getLength() + " to " + packet.getSocketAddress() +
                        "\n" + Ts3Logging.getHex(packet.getData(), packet.getLength())
        );

        datagramSocket.send(packet);
    }

    /**
     * Sends a NetworkPacket to the network, first converting it to a datagram.
     * @param header Header to send
     * @throws IOException
     */
    public void send(PacketHeader header, Packet packet) throws IOException {
        if (header == null)
            throw new NullPointerException("header");
        else if (header.getRole() != getSocketRole().getOut())
            throw new IllegalArgumentException("packet role mismatch: " +
                    header.getRole().name() + " != " +
                    getSocketRole().getOut().name());

        // Ensure type matches
        header.setType(packet.getType());

        // Construct a network packet
        NetworkPacket networkPacket = new NetworkPacket(header.getRole());
        networkPacket.setHeader(header);
        networkPacket.setPacket(packet);

        // Flush to a buffer
        ByteBuffer outputBuffer;

        if (networkPacket.getHeader().getType().isEncrypted() &&
                !networkPacket.getHeader().getPacketFlag(HeaderFlag.UNENCRYPTED)) {
            outputBuffer = getPacketTransformation().encrypt(networkPacket);
        } else {
            outputBuffer = ByteBuffer.allocate(networkPacket.getSize());

            outputBuffer.order(ByteOrder.BIG_ENDIAN);
            networkPacket.writeHeader(outputBuffer);
            networkPacket.writeBody(outputBuffer);
        }

        Ts3Logging.debug("[PROTOCOL] WRITE " + packet.getType().name());

        sendNetworkPacket(new DatagramPacket(outputBuffer.array(), outputBuffer.position()));
    }

    /**
     * Receives a datagram from the network.
     * @return Datagram.
     * @throws IOException
     */
    private DatagramPacket receiveNetworkPacket() throws IOException {
        datagramSocket.receive(datagramPacket);

        Ts3Logging.debug(
                "[NETWORK] READ Len=" + datagramPacket.getLength() + " from " + datagramPacket.getSocketAddress() +
                        "\n" + Ts3Logging.getHex(datagramPacket.getData(), datagramPacket.getLength())
        );

        return datagramPacket;
    }

    public NetworkPacket receive() throws IOException {
        while (true) {
            DatagramPacket datagramPacket = receiveNetworkPacket();

            NetworkPacket networkPacket = new NetworkPacket(getSocketRole().getIn());

            ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
            networkPacket.readHeader(buffer);

            boolean fragment = networkPacket.getHeader().getPacketFlag(HeaderFlag.FRAGMENTED);
            if (fragment && !networkPacket.getHeader().getType().isSplittable()) continue; // drop packet

            ByteBuffer packetBuffer;

            if (networkPacket.getHeader().getType().isEncrypted()) {
                packetBuffer = ByteBuffer.wrap(
                        getPacketTransformation().decrypt(
                                networkPacket.getHeader(),
                                buffer,
                                datagramPacket.getLength() - networkPacket.getHeader().getSize()
                        )
                );
            } else {
                packetBuffer = buffer;
            }

            if (fragment) {
                Ts3Logging.debug("[PROTOCOL] READ " +
                        networkPacket.getHeader().getType().name() + " fragment");

                networkPacket.setPacket(
                        new PacketFragment(
                                networkPacket.getHeader().getType(),
                                getSocketRole().getIn()
                        )
                );
            } else {
                Ts3Logging.debug("[PROTOCOL] READ " +
                        networkPacket.getHeader().getType().name());
            }


            networkPacket.readBody(packetBuffer);

            return networkPacket;
        }
    }

}
