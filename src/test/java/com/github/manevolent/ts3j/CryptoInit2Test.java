package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import static com.github.manevolent.ts3j.util.Ts3Debugging.hexStringToByteArray;

/**
 * The assertion check values SHOULD be retrieved from another known working codebase (i.e. Splamy's C# codebase)
 * for the given input license string that is in no way associated with this codebase.
 */
public class CryptoInit2Test extends TestCase {

    public static void main(String[] args) throws Exception {
        new CryptoInit2Test().testParser();
    }

    public void testParser() throws Exception {
        byte[] licenseBytes = hexStringToByteArray("0100358541498A24ACD30157918B8F50955C0DAE970AB65372CBE407" +
                "415FCF3E029B02084D15E00AA793600700000020416E6F6E796D6F7573000047D9E4DC25AA2E90ACD4DB5FA61C8F" +
                "ED369B346D84C2CA2FCCCA86F73AFEF092200A77C8810A787141");
        byte[] alpha = hexStringToByteArray("9500A5DB3B50ACECAB81");
        byte[] beta = hexStringToByteArray("EAFFC9A8BC996B25C8AA700264E99E372ECCDEB1C121D6EC0F4D49FB46" +
                "CEEBA4E3C724B3070FD70CB03D7BC08129205690ECE228CA7C");
        byte[] privateKey = hexStringToByteArray("102E591ABA4508129E812FF3437E2DDD3CA1F1EC341117CA35" +
                "14CC347A7C2A77");

        Ts3Crypt.SecureChannelParameters result =
                Ts3Crypt.cryptoInit2(licenseBytes, alpha, beta, privateKey);

        assertEquals(Ts3Debugging.getHex(result.getIvStruct()), "E4082A92F71C96A947452F5582EF2879B2051ED2D3" +
                "F2C6B0643CF5A266EE6B5180573C2F5F3F1C4AC579188366F16AE0EADC3AAF860805D8F2A831E9E49F4513");
        assertEquals(Ts3Debugging.getHex(result.getFakeSignature()), "54F2B4D661E0F9AB");
    }

}
