package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class IdentitySaveTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new IdentitySaveTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        LocalIdentity generatedIdentity = LocalIdentity.generateNew(10);
        LocalIdentity readIdentity = LocalIdentity.read(new StringBufferInputStream(generatedIdentity.export()));
    }
}
