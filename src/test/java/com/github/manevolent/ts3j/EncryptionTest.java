package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.protocol.Packet;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.ClientPacketHeader;
import com.github.manevolent.ts3j.protocol.header.HeaderFlag;
import com.github.manevolent.ts3j.protocol.packet.PacketBody2Command;
import com.github.manevolent.ts3j.protocol.packet.PacketBodyType;
import com.github.manevolent.ts3j.protocol.packet.transformation.PacketTransformation;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import javafx.util.Pair;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Base64;

public class EncryptionTest extends TestCase {
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static void main(String[] args) throws Exception {
        new EncryptionTest().testParser();
    }

    public void testParser() throws Exception {
        byte[] fakeFingerprint = Base64.getDecoder().decode("XuhegJMKpa0=");
        byte[] ivStruct = Base64.getDecoder().decode("/rn6nR71hV8eFl+15WO68fRU8pOCBw3t0FmcG5c7WNxIkeZ1NtaWTVMBde0cdU5tTKwOl8sE6gpHjnCEF4hhDw==");

        PacketTransformation transformation = new PacketTransformation(ivStruct, fakeFingerprint);

        Packet packet = new Packet(ProtocolRole.CLIENT);
        ClientPacketHeader header = new ClientPacketHeader();
        header.setType(PacketBodyType.COMMAND);
        header.setPacketFlag(HeaderFlag.NEW_PROTOCOL, true);
        header.setPacketId(1);
        packet.setHeader(header);

        Pair<byte[], byte[]> params = transformation.computeParameters(header);

        assertEquals(Base64.getEncoder().encodeToString(params.getKey()), "BF+lO776+e45u+qYAOHihg==");
        assertEquals(Base64.getEncoder().encodeToString(params.getValue()), "1IVcTMuizpDHjQgn2yGCgg==");

        packet.setBody(new PacketBody2Command(ProtocolRole.CLIENT, "clientinit client_nickname=TestClient client_version=3.1.8\\s[Build:\\s1516614607] client_platform=Windows client_input_hardware=1 client_output_hardware=1 client_default_channel= client_default_channel_password= client_server_password=QL0AFWMIX8NRZTKeof9cXsvbvu8= client_meta_data= client_version_sign=gDEgQf\\/BiOQZdAheKccM1XWcMUj2OUQqt75oFuvF2c0MQMXyv88cZQdUuckKbcBRp7RpmLInto4PIgd7mPO7BQ== client_key_offset=6199 client_nickname_phonetic= client_default_token= hwid=+LyYqbDqOvJJpN5pdAbF8\\/v5kZ0="));

        byte[] packetBytes = transformation.encrypt(packet).array();

        Ts3Debugging.debug(Base64.getEncoder().encode(packetBytes));
    }

}
