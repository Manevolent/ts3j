package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;

public class IdentityFileTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new IdentityFileTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        String identityFileContents = "[Identity]\n" +
                "id=New identity_1\n" +
                "identity=\"68616VFe2bVjeiXc+7ULXTnBkEdLZOpjIGP15CFmYbBix+T1EqQEd+WXAPNExRKA8MLl" +
                "F/MmxzWwJ3BDwrT1sie31RRwdVSUd5XVVJCTZ+EQIKGAkeZF8HUU9/VwUoWl1mKAJRHGB+QHpXMEFBa" +
                "UVBa0lBTkU2VFpDcHE1K2cvamVjbTR0K1FhNmI3VVdHR2o3Nys4ZTFQYm14Zz0=\"\n" +
                "nickname=TS3JTest\n" +
                "phonetic_nickname=\n";

        String uuid = "jJbyG5OzsAkEVGsRawC4VhLfeW4=";
        int securityLevel = 15;

        LocalIdentity localIdentity =
                LocalIdentity.read(new ByteArrayInputStream(identityFileContents.getBytes(Charset.forName("UTF8"))));

        assertEquals(uuid, localIdentity.getUid().toBase64());
        assertEquals(securityLevel, localIdentity.getSecurityLevel());
    }
}
