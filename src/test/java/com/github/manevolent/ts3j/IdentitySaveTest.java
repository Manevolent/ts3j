package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.io.File;
import java.io.StringBufferInputStream;
import java.util.Base64;

public class IdentitySaveTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new IdentitySaveTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        String testIni = "[Identity]\n" +
                "id=New identity_1\n" +
                "identity=\"171V9UfzzH5tGS4W" +
                "V8zptzxuvrqBAWUAMVNkUQJZUgl3XXYJQnBQY0" +
                "VnJAsDIgR/VgFiB0ZFBQBhcRFRQn0Jc3ZgVGZH" +
                "e0B9IlBiCjRQKw44OSJGX3t3VGZ2K3U3Xl9BLl" +
                "95KmB3S3l5QUdBaUJDWG1VWi9jQUtwQUUxbFcr" +
                "cnhNdGlaR1ZJUGIzZEtsSWx3cUVMKzVoSzVRPT" +
                "0=\"\n";

        LocalIdentity localIdentity = LocalIdentity.read(new StringBufferInputStream(testIni));

        // Assert that the output of toASN() is fixed, and matches what we expect in the above identity output
        // this does not certify encryption; this only affixes the machine-readable ASN1 format
        assertEquals(
                "MG4DAgeAAgEgAiAOvndZ5WbTh68cjFhCl" +
                "uDgPwPFh1DaJ24Sdst70SBw4wIhANPfVvKpIGi" +
                "RkPaOoYXGwoLB0SEIBSkhpMlIOPAxyyAGAiBCX" +
                "mUZ/cAKpAE1lW+rxMtiZGVIPb3dKlIlwqEL+5h" +
                "K5Q==",
                Base64.getEncoder().encodeToString(localIdentity.toASN())
        );

        String identityData = localIdentity.export(null);

        LocalIdentity localIdentity2 = LocalIdentity.read(new StringBufferInputStream(identityData));

        assertEquals(localIdentity.getSecurityLevel(), localIdentity2.getSecurityLevel());
        assertEquals(localIdentity.getPublicKeyString(), localIdentity2.getPublicKeyString());

        // If you decide to save this AND import in Teamspeak3, you need to supply a HashMap with the following:
        // phonetic_nickname
        // nickname
        // id
        //localIdentity.save(new File("out.ini"));
    }
}
