package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.util.Base64;

/**
 * Public keys get encoded to base 64 and get passed around a lot in Teamspeak, especially early on in the protocol.
 */
public class PublicKeyEncodingTest extends TestCase {
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
        new PublicKeyEncodingTest().testParser();
    }

    public void testParser() throws Exception {
        LocalIdentity keyPair = LocalIdentity.load(
                new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU="))
        );

        ECPoint point = Ts3Crypt.decodePublicKey(Base64.getDecoder().decode(keyPair.getPublicKeyString()));

        assertEquals(
                Base64.getEncoder().encodeToString(Ts3Crypt.encodePublicKey(point)),
                Base64.getEncoder().encodeToString(Ts3Crypt.encodePublicKey(keyPair.getPublicKey()))
        );
    }

}
