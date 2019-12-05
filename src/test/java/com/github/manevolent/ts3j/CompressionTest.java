package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.util.QuickLZ;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.util.Random;

/**
 * The assertion check values SHOULD be retrieved from another known working codebase (i.e. Splamy's C# codebase)
 * for the given input license string that is in no way associated with this codebase.
 */
public class CompressionTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new CompressionTest().testParser();
    }

    public void testParser() throws Exception {
        byte[] b = new byte[600];
        Random r = new Random(0x100100);

        for (int i = 0; i < 1024; i ++) {
            r.nextBytes(b);
            QuickLZ.decompress(QuickLZ.compress(b, 1), 600);
        }

        for (int i = 0; i < 1024; i ++) {
            r.nextBytes(b);
            QuickLZ.decompress(QuickLZ.compress(b, 3), 600);
        }
     }

}
