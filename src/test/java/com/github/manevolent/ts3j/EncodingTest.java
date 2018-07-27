package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;

public class EncodingTest extends TestCase {
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
        new EncodingTest().testParser();
    }

    public void testParser() throws Exception {
        byte[] encoded = "aW<#CD$)(A@$)2398i4mc20xA#W$)Aw3m403w49xw4xamw4c;w9re  __//\\uc".getBytes(Charset.forName("UTF8"));
        String base64 = Base64.getEncoder().encodeToString(encoded);
        assertEquals(base64, "YVc8I0NEJCkoQUAkKTIzOThpNG1jMjB4QSNXJClBdzNtNDAzdzQ5eHc0eGFtdzRjO3c5cmUgIF9fLy9cdWM=");
    }

}
