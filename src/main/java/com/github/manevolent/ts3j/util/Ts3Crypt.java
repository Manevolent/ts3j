package com.github.manevolent.ts3j.util;

import org.bouncycastle.asn1.*;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class Ts3Crypt {
    private static ECDomainParameters getDomainParameters() {
        ECNamedCurveParameterSpec ecp = ECNamedCurveTable.getParameterSpec("prime256v1");
        return new ECDomainParameters(ecp.getCurve(), ecp.getG(), ecp.getN(), ecp.getH(), ecp.getSeed());
    }

    public static boolean verifySignature(ECPoint publicKey, byte[] data, byte[] signature) {
        DSADigestSigner signer = new DSADigestSigner(new ECDSASigner(), new SHA256Digest());
        ECPublicKeyParameters signingKey = new ECPublicKeyParameters(publicKey, getDomainParameters());

        signer.init(false, signingKey);
        signer.update(data, 0, data.length);

        return signer.verifySignature(signature);
    }

    /**
     * From http://stackoverflow.com/questions/2409618/how-do-i-decode-a-der-encoded-string-in-java
     */
    private static ASN1Encodable[] readDERObject(byte[] data) throws IOException
    {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        ASN1InputStream asnInputStream = new ASN1InputStream(inStream);

        ASN1Object object = asnInputStream.readObject();

        if (object instanceof DLSequence) {
            return ((DLSequence) object).toArray();
        } else {
            return new ASN1Encodable[] { object };
        }
    }

    public static ECPoint decodePublicKey(byte[] bytes) {
        try {
            DERSequence asnKeyData = new DERSequence(readDERObject(bytes));

            BigInteger x = ((org.bouncycastle.asn1.ASN1Integer)asnKeyData.getObjectAt(2)).getValue();
            BigInteger y = ((org.bouncycastle.asn1.ASN1Integer)asnKeyData.getObjectAt(3)).getValue();

            return getDomainParameters().getCurve().createPoint(x, y);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encodePublicKey(ECPoint publicKey) {
        try {
            byte[] dataArray = new DERSequence(new ASN1Encodable[]{
                    new DERBitString(new byte[]{0b0000_0000}, 7),
                    new DERInteger(32),
                    new DERInteger(publicKey.getAffineXCoord().toBigInteger()),
                    new DERInteger(publicKey.getAffineYCoord().toBigInteger())
            }).getEncoded();

            return dataArray;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
