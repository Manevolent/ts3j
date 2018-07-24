package com.github.manevolent.ts3j.protocol.packet.transformation;

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
        temporaryByteBuffer.put((byte) 0); // TODO generation

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

    public byte[] encrypt(PacketHeader header, byte[] body) {
        return transform(header, body, true);
    }

    public byte[] decrypt(PacketHeader header, byte[] body) {
        return transform(header, body, false);
    }

    private byte[] transform(PacketHeader header, byte[] body, boolean encrypt) {
        Pair<byte[], byte[]> parameters = computeParameters(header);

        byte[] associatedText = new byte[8];

        CipherParameters cipherParameters = new AEADParameters(new KeyParameter(
                parameters.getKey()),
                8 * associatedText.length,
                parameters.getValue(),
                encrypt ? associatedText : header.getMac()
        );

        byte[] result;
        int len;
        synchronized (cipher){
            cipher.init(encrypt, cipherParameters);

            result = new byte[cipher.getOutputSize(body.length)];
            len = cipher.processBytes(body, 0, body.length, result, 0);

            try {
                len += cipher.doFinal(result, len);
            } catch (InvalidCipherTextException e) {
                throw new RuntimeException(e);
            }
        }

        byte[] output = new byte[len];
        System.arraycopy(result, 0, output, 0, len);

        if (encrypt)
            header.setMac(associatedText);

        return output;
    }
}
