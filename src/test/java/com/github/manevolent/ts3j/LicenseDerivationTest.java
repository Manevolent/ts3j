package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.license.License;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Tests license derivation.  Licenses are part of generating a shared key.  The seed for the randomness is an
 * ephemeral license at the tail end of a license block.  That jumbles up the registers in the hash of the block,
 * which is used in the protocol to generate the shared AES keys.  What we're doing here is making sure that derivation
 * happens appropriately and continues to in the future.
 *
 * The assertion check values SHOULD be retrieved from another known working codebase (i.e. Splamy's C# codebase)
 * for the given input license string that is in no way associated with this codebase.
 */
public class LicenseDerivationTest extends TestCase {
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
        new LicenseDerivationTest().testParser();
    }

    private void check(String base64License, String expectedResult) {
        assertEquals(Base64.getEncoder().encodeToString(
                License.deriveKey(License.readLicenses(ByteBuffer.wrap(
                        Base64.getDecoder().decode(base64License)
                )))
        ), expectedResult);
    }

    public void testParser() throws Exception {
        check("AQA1hUFJiiSs0wFXkYuPUJVcDa6XCrZTcsvkB0Ffzz4CmwIITRXgCqeTYA" +
                "cAAAAgQW5vbnltb3VzAADSN9wlGHZEHZvX7ImHoqYezibj5byDh0f4oMsG3afDxyAKePI" +
                "VCnma1Q==",
                "z/bYm6TmHmuAil/osx8eGi6Oits2vIO4i6Bm13RuiGg=");

        check("AQA1hUFJiiSs0wFXkYuPUJVcDa6XCrZTcsvkB0Ffzz4C" +
                        "mwIITRXgCqeTYAcAAAAgQW5vbnltb3VzAABx1YQfzCiB8b" +
                        "ZZAdGwXNTLmdhiOpjaH3OOlISy5vrM3iAKePBVCnmZFQ==",
                "lrukIi392D7ltdKFp5mURT3Ydk+oWYNjMt3kptbQl6I=");


        check("AQA1hUFJiiSs0wFXkYuPUJVcDa6XCrZTcsvkB0Ffzz4CmwIITR" +
                "XgCqeTYAcAAAAgQW5vbnltb3VzAAAK5C0l+xtOTAZGEA/GHHOySAUEBmq7fN5" +
                "PG7uSGPEADiAKePGHCnmaRw==",
                "H+UcEreBUkCWN18nTYZp0QQkQqGA8IqzqvJ5qB225Z8=");

        check("AQCvbHFTQDY/terPeilrp/ECU9xCH5U3xC92lYTNaY/0KQ" +
                "AJFueAazbsgAAAACVUZWFtU3BlYWsgU3lzdGVtcyBHbWJIAACOaeEfqmGhp2qXAO" +
                "QS7IfKw939kEy1i6NSE0OsO4IIPgAJRa7CL1XtIQAAACRUZWFtU3BlYWsgc3lzdGVtcy" +
                "BHbWJIAACvbItqlhq4qOHjWFNky0ASbs2BBL//QMVspoGLWmXhzQIJSQCAH" +
                "/mxgAYAAAAAVGVhbVNwZWFrIFN5c3RlbXMgR21iSAAAWysE+/5ug1yKk4WiiWMNeB+" +
                "E+vQ5RmxglJLi8rW15UsgCnjyQAp5mwA=",
                "ZWGGM5QmRdl6AQOgBUZgOAVHcicsm/YPYR3EukrFiGE=");
    }

}
