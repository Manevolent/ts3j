package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.license.License;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.nio.ByteBuffer;

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

    public void testParser() throws Exception {
        byte[] licenseBytes = hexStringToByteArray("0100358541498A24ACD30157918B8F50955C0DAE970AB65372CBE407415FCF3E029B02084D15E00AA793600700000020416E6F6E796D6F75730000AA43CD339C6E30E4E848F2FF031B93146B2E4925B28B33A3E8054D2101D06FF7200A776C5E0A78151E");
        byte[] derrivedKey = License.deriveKey(License.readLicenses(ByteBuffer.wrap(licenseBytes)));
        assertEquals(Ts3Debugging.getHex(derrivedKey), "B4EBE337EA2AA9F37C3E2FF8889D24B08B7439E654513B8BF342D1D174895C30");

    }

}
