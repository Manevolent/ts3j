package com.github.manevolent.ts3j.identity;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.*;

public class LocalIdentity extends Identity {
    private final BigInteger privateKey;

    public LocalIdentity(ECPoint publicKey, BigInteger privateKey) {
        super(publicKey);

        this.privateKey = privateKey;
    }

    public static LocalIdentity load(ECPoint publicKey,
                                     BigInteger privateKey) {
        return new LocalIdentity(publicKey, privateKey);
    }

    public static LocalIdentity load(BigInteger privateKey) {
        return new LocalIdentity(Ts3Crypt.generatePublicKeyFromPrivateKey(privateKey), privateKey);
    }

    public static LocalIdentity generateNew(int securityLevel) throws GeneralSecurityException {
        ECNamedCurveParameterSpec ecp = ECNamedCurveTable.getParameterSpec("prime256v1");
        ECDomainParameters domainParams =
                new ECDomainParameters(ecp.getCurve(), ecp.getG(), ecp.getN(), ecp.getH(), ecp.getSeed());
        ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(domainParams, new SecureRandom());

        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(keyGenParams);

        AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) keyPair.getPrivate();
        ECPublicKeyParameters publicKey = (ECPublicKeyParameters) keyPair.getPublic();

        LocalIdentity localIdentity = load(publicKey.getQ().normalize(), privateKey.getD());
        localIdentity.improveSecurity(securityLevel);

        return localIdentity;
    }

    public byte[] sign(byte[] data) {
        return Ts3Crypt.createSignature(privateKey, data);
    }

    public BigInteger getPrivateKey() {
        return privateKey;
    }
}
