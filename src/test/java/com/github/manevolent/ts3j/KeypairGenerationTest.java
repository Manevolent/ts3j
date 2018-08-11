package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.util.Pair;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import junit.framework.TestCase;

import java.util.Base64;

public class KeypairGenerationTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new KeypairGenerationTest().testParser();
    }

    public void testParser() throws Exception {
        byte[] staticPrivateKey = Base64.getDecoder().decode("3fjdzQxJyRWLzWPAKzbHRnfWZ2sP5HAcFm7ovWZ0iVE=");

        Pair<byte[], byte[]> keypair = Ts3Crypt.generateKeypair(staticPrivateKey);

        assertEquals(
                "2PjdzQxJyRWLzWPAKzbHRnfWZ2sP5HAcFm7ovWZ0iVE=",
                Base64.getEncoder().encodeToString(keypair.getValue())
        );

        assertEquals(
                "DDmF1AgysRHsAxegbsH1r3h4FzupxQgvHQfrOLAQRaY=",
                Base64.getEncoder().encodeToString(keypair.getKey())
        );
    }

}
