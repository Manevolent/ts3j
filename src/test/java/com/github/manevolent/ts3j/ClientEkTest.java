package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Base64;

/**
 * This is a self proof to make sure we can encrypt and decrypt our OWN data.  This does not prove that the encrypted
 * data would be correct.  For that, there are other tests.
 */
public class ClientEkTest extends TestCase {
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
        new ClientEkTest().testParser();
    }

    public void testParser() throws Exception {
        LocalIdentity keyPair = LocalIdentity.load(
                new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJ" +
                        "nRUl4auQf8+znjYrOmWU="))
        );

        byte[] key = hexStringToByteArray("5763FA00EBC38DCE3953AFCACD8CFF2209208D15BB7D" +
                "9B699D61A5675EDA9370");
        byte[] beta = hexStringToByteArray("EA581E7CBADED7C47B9CBA3D3EEFB33A49B61ED0F0B45DF" +
                "50D900C12F551B5F6B0EFEE3F593FB225184C78C06DB2D688669B70ABAC6D");

        byte[] signature = Ts3Crypt.generateClientEkProof(key, beta, keyPair.getPrivateKey());

        assertEquals(true, Ts3Crypt.verifyClientEkProof(key, beta, signature, keyPair.getPublicKey()));
    }

}
