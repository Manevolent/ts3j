package com.github.manevolent.ts3j;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import junit.framework.TestCase;

import static com.github.manevolent.ts3j.util.Ts3Debugging.hexStringToByteArray;

public class CryptoInit2Test extends TestCase {

    public static void main(String[] args) throws Exception {
        new CryptoInit2Test().testParser();
    }

    public void testParser() throws Exception {
        byte[] licenseBytes = hexStringToByteArray("0100358541498A24ACD30157918B8F50955C0DAE970AB65372CBE407415FCF3E029B02084D15E00AA793600700000020416E6F6E796D6F7573000047D9E4DC25AA2E90ACD4DB5FA61C8FED369B346D84C2CA2FCCCA86F73AFEF092200A77C8810A787141");
        byte[] alpha = hexStringToByteArray("9500A5DB3B50ACECAB81");
        byte[] omega = hexStringToByteArray("304B03020700020120022025D13CF0C5A17A467961B04418E015941D6C6558DA6AEC79CFFDDA1DEF8AB55202200F3B3F0D3A4F1C5C02AD1F9A3A5114AC58E89905792FBB83AD4FFE7F82ACAADA");
        byte[] proof = hexStringToByteArray("3046022100F4B921DDB2C9737D9AB100BA251D11216DF1601C62AF17150E6FC29A2C6EEC0F022100FC8948E595BBD895A2FDFE0EB22A5241655BD47591F8D2733035CF76A72245AD");
        byte[] beta = hexStringToByteArray("EAFFC9A8BC996B25C8AA700264E99E372ECCDEB1C121D6EC0F4D49FB46CEEBA4E3C724B3070FD70CB03D7BC08129205690ECE228CA7C");
        byte[] privateKey = hexStringToByteArray("102E591ABA4508129E812FF3437E2DDD3CA1F1EC341117CA3514CC347A7C2A77");

        Ts3Crypt.SecureChannelParameters result =
                Ts3Crypt.cryptoInit2(licenseBytes, alpha, omega, proof, beta, privateKey);

        assertEquals(Ts3Debugging.getHex(result.getIvStruct()), "E4082A92F71C96A947452F5582EF2879B2051ED2D3F2C6B0643CF5A266EE6B5180573C2F5F3F1C4AC579188366F16AE0EADC3AAF860805D8F2A831E9E49F4513");
        assertEquals(Ts3Debugging.getHex(result.getFakeSignature()), "54F2B4D661E0F9AB");
    }

}
