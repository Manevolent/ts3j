package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import junit.framework.TestCase;

import java.math.BigInteger;
import java.util.Base64;

public class ClientUidGenerationTest extends TestCase {
    public static void main(String[] args) throws Exception {
        new ClientUidGenerationTest().testParser();
    }

    public void testParser() throws Exception {
        LocalIdentity keyPair = LocalIdentity.load(
                new BigInteger(Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU="))
        );

        keyPair.improveSecurity(10);

        assertEquals(2294, keyPair.getKeyOffset());

        assertEquals(2295, keyPair.getLastCheckedKeyOffset());

        assertEquals("MEsDAgcAAgEgAiBpPRbTliVt9KxtIz8saYdwcnNgcwaKLb" +
                "KYSpDNO87u9gIgSWWPKcSJ9P6VZKJfRdpWwcfMdJv+NA9/hXUtz1uwRVI=", keyPair.getPublicKeyString());

        assertEquals("27QHSPRzQe+enxh56eUokkLmsFg=", keyPair.getUid().toBase64());

        assertEquals(
                "MEsDAgcAAgEgAiBpPRbTliVt9KxtIz8saYdwcnNgcwaKLbKY" +
                "SpDNO87u9gIgSWWPKcSJ9P6VZKJfRdpWwcfMdJv+NA9/hXUtz1uwRVI=",
                keyPair.getPublicKeyString());
    }

}
