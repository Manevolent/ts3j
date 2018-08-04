package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import java.util.Base64;

public class GenerateIdentityTest {
    public static void main(String[] args) throws Exception {
        try {
            Ts3Debugging.setEnabled(true);

            LocalIdentity identity = LocalIdentity.generateNew(0);

            Ts3Debugging.debug("Private key: " +
                    Base64.getEncoder().encodeToString(identity.getPrivateKey().toByteArray())
            );

            identity.improveSecurity(1000);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
