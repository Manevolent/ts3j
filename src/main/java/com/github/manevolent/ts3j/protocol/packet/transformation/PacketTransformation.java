package com.github.manevolent.ts3j.protocol.packet.transformation;

import com.github.manevolent.ts3j.protocol.NetworkPacket;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.protocol.header.PacketHeader;
import com.github.manevolent.ts3j.util.Ts3Logging;
import javafx.util.Pair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PacketTransformation {
    private static final int MAC_LEN = 8;
    private static MessageDigest SHA256;

    {
        try {
            SHA256 = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final byte[] key;

    private final EAXBlockCipher cipher;

    public PacketTransformation(byte[] key) {
        this.key = key;
        this.cipher = new EAXBlockCipher(new AESEngine());
    }

    public Pair<byte[], byte[]> computeParameters(PacketHeader header) {
        ByteBuffer temporaryByteBuffer = ByteBuffer
                .allocate(key.length == 20 ? 26 : 70)
                .order(ByteOrder.BIG_ENDIAN);

        temporaryByteBuffer.put((byte) (header.getRole() == ProtocolRole.SERVER ? 0x30 : 0x31));
        temporaryByteBuffer.put((byte) (header.getType().getIndex() & 0xFF));
        temporaryByteBuffer.put((byte) (header.getGeneration() & 0xFF));

        if (key.length == 20) {
            //temporary[6 - 26] = SIV[0 - 20]
            temporaryByteBuffer.put(key, 0, 20);
        } else {
            //temporary[6 - 70] = SIV[0 - 64]
            temporaryByteBuffer.put(key, 0, 64);
        }

        byte[] key_nonce = SHA256.digest(temporaryByteBuffer.array());

        byte[] key = new byte[16];
        byte[] nonce = new byte[16];

        System.arraycopy(key_nonce, 0, key, 0, 16);
        System.arraycopy(key_nonce, 16, nonce, 0, 16);

        // key[0]          = key[0] xor ((PId & 0xFF00) >> 8)
        // key[1]          = key[1] xor ((PId & 0x00FF) >> 0)

        key[0] = (byte) (key[0] ^ ((byte) (header.getType().getIndex() & 0xFF00) >> 8));
        key[1] = (byte) (key[0] ^ ((byte) (header.getType().getIndex() & 0x00FF) >> 0));

        return new Pair<>(key, nonce);
    }

    public ByteBuffer encrypt(NetworkPacket packet) {
        Pair<byte[], byte[]> parameters = computeParameters(packet.getHeader());

        // Write header (this is temporary)
        ByteBuffer headerBuffer = packet.writeHeader(
                ByteBuffer
                .allocate(packet.getHeader().getSize())
                .order(ByteOrder.BIG_ENDIAN)
        );

        // Get the header without a MAC for the associated text field
        byte[] headerWithoutMac = new byte[packet.getHeader().getSize() - MAC_LEN];
        System.arraycopy(headerBuffer.array(), MAC_LEN, headerWithoutMac, 0, headerWithoutMac.length);

        CipherParameters cipherParameters = new AEADParameters(
                new KeyParameter(parameters.getKey()),
                8 * MAC_LEN,
                parameters.getValue(),
                headerWithoutMac
        );

        int dataLen = packet.getPacket().getSize();
        ByteBuffer packetBuffer = packet.writeBody(ByteBuffer.allocate(dataLen).order(ByteOrder.BIG_ENDIAN));

        byte[] result;
        int len;

        synchronized (cipher) {
            cipher.init(true, cipherParameters);

            result = new byte[cipher.getOutputSize(dataLen)];
            len = cipher.processBytes(packetBuffer.array(), 0, dataLen, result, 0);

            try {
                len += cipher.doFinal(result, len);
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException(e);
            }
        }

        ByteBuffer outputBuffer = ByteBuffer.allocate(MAC_LEN + headerWithoutMac.length + (len - MAC_LEN));

        // MAC
        outputBuffer.put(result, len - MAC_LEN, MAC_LEN);

        // Rest of header
        outputBuffer.put(headerWithoutMac);

        Ts3Logging.debug("WRITE HEADER: " + Ts3Logging.getHex(outputBuffer.array(), headerBuffer.limit()));

        // Encrypted body
        outputBuffer.put(result, 0, len - MAC_LEN);

        return outputBuffer;
    }

    public byte[] decrypt(PacketHeader header, ByteBuffer buffer, int dataLen) {
        Pair<byte[], byte[]> parameters = computeParameters(header);

        byte[] headerWithoutMac = new byte[header.getSize() - MAC_LEN];
        System.arraycopy(buffer.array(), MAC_LEN, headerWithoutMac, 0, headerWithoutMac.length);

        // Get the header without a MAC for the associated text field
        CipherParameters cipherParameters = new AEADParameters(
                new KeyParameter(parameters.getKey()),
                8 * MAC_LEN,
                parameters.getValue(),
                headerWithoutMac
        );

        byte[] result;
        int len;

        synchronized (cipher) {

            cipher.init(false, cipherParameters);
            result = new byte[cipher.getOutputSize(dataLen + MAC_LEN)];

            len = cipher.processBytes(buffer.array(), header.getSize(), dataLen, result, 0);
            len += cipher.processBytes(buffer.array(), 0, MAC_LEN, result, len);
            try {
                len += cipher.doFinal(result, len);
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException(e);
            }

            if (len != dataLen)
                throw new IllegalArgumentException(len + " != " + dataLen);

        }


        return result;
    }
}
