package com.github.manevolent.ts3j.protocol.socket.client;

import com.github.manevolent.ts3j.identity.Identity;
import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.packet.PacketBody;
import com.github.manevolent.ts3j.protocol.packet.handler.PacketHandler;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.protocol.socket.TeamspeakSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public interface TeamspeakClientSocket extends TeamspeakSocket {

    Map<String, Object> getOptions();

    default <T extends Object> T getOption(String key, Class<T> clazz) {
        Object value = getOptions().get(key);

        if (value == null) return  null;

        else if (!value.getClass().isAssignableFrom(clazz)) {
            throw new ClassCastException(
                    clazz.getName()
                            + " is not assignable to option type " +
                            value.getClass().getName()
            );
        } else
            return (T) value;
    }

    default Object setOption(String key, Object value) {
        return getOptions().put(key, value);
    }

    boolean isConnected();

    Identity getIdentity();

    void setIdentity(Identity identity);

    PacketTransformation getTransformation();

    void setTransformation(PacketTransformation transformation);

    PacketHandler getHandler();

    /**
     * Reads a packet from the remote endpoint.
     *
     * Note that this call may fight with any other reading threads; it is advisede to use the PacketHandler instead.
     *
     * This method does not return ACKs, such as: ACK, ACK_LOW, PING, or PONG.  These are protocol-level packets that
     * are handled automatically by the protocol code.
     *
     * This method does return INIT1, COMMAND, COMMAND_LOW, and possibly VOICE, and VOICE_WHISPER.
     *
     * This method does not return fragments or encrypted bodies.  This is handled by the underlying protocol
     * implementation.
     *
     * @return Packet.
     * @throws IOException
     * @throws TimeoutException
     */
    Packet readPacket() throws IOException, TimeoutException;

    /**
     * Sends a packet body to the remote endpoint, generating header information as necessary.
     *
     * @param body Body to send.
     * @throws IOException if there is an issue sending the packet (i.e. socket closed)
     * @throws TimeoutException if the packet is not acknowledged in time
     */
    void writePacket(PacketBody body) throws IOException, TimeoutException;

    /**
     * Writes a packet to the remote endpoint, not generating header information.  This must be done by the caller.
     * The packet will be checked, split, and encrypted as needed.
     *
     * This includes packet ID, client ID, type, and flags.
     *
     * @param packet raw packet information to send.
     * @throws IOException
     * @throws TimeoutException
     */
    void writePacket(Packet packet) throws IOException, TimeoutException;

}
