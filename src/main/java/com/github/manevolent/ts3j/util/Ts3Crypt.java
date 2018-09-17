package com.github.manevolent.ts3j.util;

import Punisher.NaCl.Internal.Ed25519Ref10.GroupElementP2;
import Punisher.NaCl.Internal.Ed25519Ref10.GroupElementP3;
import Punisher.NaCl.Internal.Ed25519Ref10.GroupOperations;
import Punisher.NaCl.Internal.Ed25519Ref10.ScalarOperations;
import Punisher.NaCl.Internal.Sha512;
import com.github.manevolent.ts3j.api.Message;
import com.github.manevolent.ts3j.identity.*;
import com.github.manevolent.ts3j.license.License;
import com.github.manevolent.ts3j.util.Pair;
import org.bouncycastle.asn1.*;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import java.util.List;

public final class Ts3Crypt {
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public static byte[] hash128(byte[] data, int offs, int len) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.update(data, offs, len);
            return digest.digest();
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] hash128(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA1").digest(data);
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] hash256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA256").digest(data);
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static byte[] hash512(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA512").digest(data);
        } catch (GeneralSecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Ts3Crypt.SecureChannelParameters cryptoInit(byte[] alpha, byte[] beta, byte[] omega,
                                                              LocalIdentity identity)
            throws IOException {
        if (beta.length != 10 && beta.length != 54)
            throw new IOException("Invalid beta size: " + beta.length + " != (10,54)");

        // getSharedSecret
        byte[] sharedKey = getSharedSecret(omega, identity);

        // Splamy's setSharedSecret
        byte[] ivStruct = new byte[10 + beta.length];

        //XorBinary(sharedKey, alpha, alpha.Length, ivStruct);
        xor(sharedKey, 0, alpha, 0, alpha.length, ivStruct, 0);


        //XorBinary(sharedKey.Slice(10), beta, beta.Length, ivStruct.AsSpan(10));
        xor(sharedKey, 10, beta, 0, beta.length, ivStruct, 10);


        byte[] buffer2 = hash128(ivStruct, 0, ivStruct.length);
        byte[] fakeSignature = new byte[8];
        System.arraycopy(buffer2, 0, fakeSignature, 0, 8);

        return new SecureChannelParameters(fakeSignature, ivStruct);
    }

    public static byte[] getSharedSecret(byte[] omega, LocalIdentity identity) {
        ECPoint publicKeyPoint = Ts3Crypt.decodePublicKey(omega);
        ECPoint p = publicKeyPoint.multiply(identity.getPrivateKey()).normalize();

        byte[] keyArr = p.getAffineXCoord().toBigInteger().toByteArray();
        byte[] sharedSecret;
        if (keyArr.length == 32)
            sharedSecret = Ts3Crypt.hash128(keyArr);
        else if (keyArr.length > 32)
            sharedSecret = Ts3Crypt.hash128(keyArr, keyArr.length - 32, 32);
        else {
            byte[] keyArrExt = new byte[32];
            System.arraycopy(keyArr, 0, keyArrExt, 32 - keyArr.length, keyArr.length);
            sharedSecret = Ts3Crypt.hash128(keyArrExt);
        }

        return sharedSecret;
    }

    public static Ts3Crypt.SecureChannelParameters cryptoInit2(byte[] license, byte[] alpha,
                                                               byte[] beta, byte[] privateKey) {
        // 3.2.2.2 Parsing the license
        List<License> licenses = License.readLicenses(ByteBuffer.wrap(license));

        byte[] key = License.deriveKey(licenses);
        byte[] sharedSecret = Ts3Crypt.generateSharedSecret2(key, privateKey);

        Ts3Crypt.SecureChannelParameters parameters =
                Ts3Crypt.getSecureParameters(alpha, beta, sharedSecret);

        return parameters;
    }

    private static void xor(byte[] a, int aoffs, byte[] b, int boffs, int len, byte[] outBuf, int outOffs)
    {
        if (a.length < len || b.length < len || outBuf.length < len)
            throw new ArrayIndexOutOfBoundsException();

        for (int i = 0; i < len; i++)
            outBuf[i + outOffs] = (byte)(a[i + aoffs] ^ b[i + boffs]);
    }

    public static byte[] generateClientEkProof(byte[] key, byte[] beta,
                                               LocalIdentity identity) {
        return generateClientEkProof(key, beta, identity.getPrivateKey());
    }

    public static byte[] generateClientEkProof(byte[] key, byte[] beta,
                                               BigInteger privateKey) {
        // generate proof for clientek
        byte[] data = new byte[86];
        System.arraycopy(key, 0, data, 0, 32);
        System.arraycopy(beta, 0, data, 32, 54);

        return createSignature(privateKey, data);
    }

    public static boolean verifyClientEkProof(byte[] key, byte[] beta, byte[] signature, ECPoint publicKey) {
        // generate proof for clientek
        byte[] data = new byte[86];
        System.arraycopy(key, 0, data, 0, 32);
        System.arraycopy(beta, 0, data, 32, 54);

        return verifySignature(publicKey, data, signature);
    }

    public static SecureChannelParameters getSecureParameters(byte[] alpha, byte[] beta, byte[] sharedKey) {
        if (beta.length != 10 && beta.length != 54)
            throw new IllegalArgumentException("invalid beta size (" + beta.length + ")");

        byte[] fakeSignature = new byte[8];
        byte[] ivStruct = new byte[10 + beta.length];

        xor(sharedKey, 0, alpha, 0, alpha.length, ivStruct, 0);
        xor(sharedKey, 10, beta, 0, beta.length, ivStruct, 10);

        byte[] buffer = hash128(ivStruct);

        System.arraycopy(buffer, 0, fakeSignature, 0, 8);

        return new SecureChannelParameters(fakeSignature, ivStruct);
    }

    public static byte[] generateSharedSecret2(byte[] publicKey, byte[] privateKey) {
        byte[] privateKeyCopy = new byte[32];
        System.arraycopy(privateKey, 0, privateKeyCopy, 0, 32);

        privateKeyCopy[31] &= 0x7F;

        GroupElementP3 pub1 = new GroupElementP3();
        GroupOperations.ge_frombytes_negate_vartime(pub1, publicKey, 0);

        GroupElementP2 mul = new GroupElementP2();
        GroupOperations.ge_scalarmult_vartime(mul, privateKeyCopy, pub1);

        byte[] sharedTmp = new byte[32];
        GroupOperations.ge_tobytes(sharedTmp, 0, mul);

        sharedTmp[31] ^= 0x80;

        try {
            return Sha512.Hash(sharedTmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public static byte[] createSignature(BigInteger privateKey, byte[] data) {
        DSADigestSigner signer = new DSADigestSigner(new ECDSASigner(), new SHA256Digest());
        ECPrivateKeyParameters signingKey = new ECPrivateKeyParameters(privateKey, getDomainParameters());

        signer.init(true, signingKey);
        signer.update(data, 0, data.length);

        return signer.generateSignature();
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

    public static Pair<byte[], byte[]> generateKeypair() {
        byte[] privateKey = new byte[32];
        new SecureRandom().nextBytes(privateKey);
        return generateKeypair(privateKey);
    }

    public static Pair<byte[], byte[]> generateKeypair(byte[] privateKey) {
        ScalarOperations.sc_clamp(privateKey, 0);

        GroupElementP3 A = new GroupElementP3();
        GroupOperations.ge_scalarmult_base(A, privateKey, 0);

        byte[] publicKey = new byte[32];
        GroupOperations.ge_p3_tobytes(publicKey, 0, A);

        return new Pair<>(publicKey, privateKey);
    }

    public static ECPoint generatePublicKeyFromPrivateKey(BigInteger privateKey) {
        return ECNamedCurveTable.getParameterSpec("prime256v1").getG().multiply(privateKey).normalize();
    }

    public static final class SecureChannelParameters {
        private final byte[] fakeSignature;
        private final byte[] ivStruct;

        public SecureChannelParameters(byte[] fakeSignature, byte[] ivStruct) {
            this.fakeSignature = fakeSignature;
            this.ivStruct = ivStruct;
        }

        public byte[] getFakeSignature() {
            return fakeSignature;
        }

        public byte[] getIvStruct() {
            return ivStruct;
        }
    }
}
