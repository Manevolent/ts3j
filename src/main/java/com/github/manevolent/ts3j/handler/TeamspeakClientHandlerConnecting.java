package com.github.manevolent.ts3j.handler;

import com.github.manevolent.ts3j.TeamspeakClient;
import com.github.manevolent.ts3j.enums.PacketType;
import com.github.manevolent.ts3j.util.Ts3Logging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class TeamspeakClientHandlerConnecting extends TeamspeakClientHandler {


    public TeamspeakClientHandlerConnecting(TeamspeakClient client) {
        super(client);
    }

    @Override
    public void onAssigned() throws IOException {
        Ts3Logging.debug("Connecting: sending Init1...");

        processInit1();
    }

    /**
     * Processes the first client dispatch of the Init1 packet (C2S initial handshake packet)
     */
    public void processInit1() throws IOException {
        processInit1(null);
    }

    public void processInit1(byte[] data) throws IOException {
        int versionLen = 4;
        int initTypeLen = 1;
        Integer type = null;

        if (data != null) {
            type = (int) data[0];

            if (data.length < initTypeLen)
                throw new IllegalArgumentException("invalid Init1 packet (too short)");
        }

        ByteBuffer buffer = null;

        if (type == null) {
            buffer = ByteBuffer.allocate(versionLen + initTypeLen + 4 + 4 + 8);

            buffer.put(new byte[] { 0x09, (byte)0x83, (byte)0x8C, (byte)0xCF });
            buffer.put((byte) 0x00); // initType
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putInt((int) (System.currentTimeMillis() / 1000L));

            Random random = new Random();
            for (int i = 0; i < 4; i++)
                buffer.put((byte) random.nextInt(256)); // 4byte random
        } else {
            switch (type) {
                case 1:

                    break;
                case 3:

                    break;
                case 0x7F:
                    // 0x7F: Some strange servers do this
                    // the normal client responds by starting again
                    break;
                default:
                    throw new IllegalArgumentException("invalid Init1 packet id: " + type);
            }
        }

        if (buffer == null || buffer.position() <= 0)
            throw new IllegalStateException("invalid send buffer");

        getClient().send(PacketType.INIT, buffer.array());
    }
}
