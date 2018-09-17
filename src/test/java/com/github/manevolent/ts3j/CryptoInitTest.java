package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.identity.LocalIdentity;
import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;
import org.bouncycastle.util.encoders.Base64;

import java.math.BigInteger;

import static com.github.manevolent.ts3j.util.Ts3Debugging.hexStringToByteArray;

/**
 * The assertion check values SHOULD be retrieved from another known working codebase (i.e. Splamy's C# codebase)
 * for the given input license string that is in no way associated with this codebase.
 */
public class CryptoInitTest extends TestCase {

    public static void main(String[] args) throws Exception {
        new CryptoInitTest().testParser();
    }

    public void testParser() throws Exception {
        Ts3Debugging.setEnabled(true);

        LocalIdentity identity = LocalIdentity.load(
                new BigInteger(java.util.Base64.getDecoder().decode("Tj6YXM3qyRv8n25L2pH+OEJnRUl4auQf8+znjYrOmWU="))
        );

        byte[] alpha = Base64.decode("p0SD9rgLfAzMvQ==");
        byte[] beta = Base64.decode("gWw6y53Sft6JFA==");
        byte[] omega = Base64.decode(
                "MEwDAgcAAgEgAiAzliV+Hdt3q4PJ3G7+sA+lJdyz4pemWxwbZ" +
                "4VwUQtf3gIhAOIK7NxDEA4c54o8fu0mPjRShjWxQI6vW5ExD44JlPXb"
        );

        assertEquals("wd/BG5pGG5zx05V7XDhtf+zhL38=", Base64.toBase64String(Ts3Crypt.getSharedSecret(omega, identity)));

        Ts3Crypt.SecureChannelParameters parameters = Ts3Crypt.cryptoInit(
                alpha,
                beta,
                omega,
                identity
        );


        assertEquals("ZptC7SJNZ5A9bhQXZvPwrZI/pms=", Base64.toBase64String(parameters.getIvStruct()));

        assertEquals("ZbeTrqtllww=", Base64.toBase64String(parameters.getFakeSignature()));
    }

}
