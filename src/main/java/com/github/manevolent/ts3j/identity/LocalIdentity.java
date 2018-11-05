package com.github.manevolent.ts3j.identity;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.ini4j.Ini;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalIdentity extends Identity {
    /**
     * Pattern used for Identity.identity value in TS3 identity INI files.
     */
    private static final Pattern identityPattern = Pattern.compile("^\"(\\d+)V([\\w\\/\\+]+={0,2})\"$", Pattern.MULTILINE);

    /**
     * Key used to obfuscate the key component(s) in official TS3 identity INI files.
     */
    private static final byte[] identityFileObfuscationKey =
            ("b9dfaa7bee6ac57ac7b65f1094a1c155e747327bc2fe5d5" +
                    "1c512023fe54a280201004e90ad1daaae1075d53" +
                    "b7d571c30e063b5a62a4a017bb394833aa0983e6e").getBytes(Charset.forName("ASCII"));
    /**
     * Identity private component.
     */
    private final BigInteger privateKey;

    public LocalIdentity(ECPoint publicKey, BigInteger privateKey) {
        super(publicKey);

        this.privateKey = privateKey;
    }

    /**
     * Loads an identity from a specific key-pair.
     * @param publicKey public key component
     * @param privateKey private key component
     * @return local identity with given components
     */
    public static LocalIdentity load(ECPoint publicKey,
                                     BigInteger privateKey) {
        return new LocalIdentity(publicKey, privateKey);
    }

    /**
     * Loads an identity, re-generating the public component from the private component.
     * @param privateKey private key component
     * @return local identity with given components
     */
    public static LocalIdentity load(BigInteger privateKey) {
        return new LocalIdentity(Ts3Crypt.generatePublicKeyFromPrivateKey(privateKey), privateKey);
    }

    /**
     * Generates a new identity with a given security level target.
     * @param securityLevel security level to generate for (may take time)
     * @return local identity with given security level
     * @throws GeneralSecurityException
     */
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

    /**
     * Gets the identity's private component.
     * @return private key.
     */
    public BigInteger getPrivateKey() {
        return privateKey;
    }

    /**
     * Reads a TS3 identity from a given TS3 identity INI file.
     * @param iniFile INI file to read
     * @return local identity
     */
    public static final LocalIdentity read(File iniFile) throws IOException {
        return read(new FileInputStream(iniFile));
    }

    /**
     * Reads a TS3 identity from a given input stream.
     * @param inputStream Input stream to read
     * @return local identity
     */
    public static final LocalIdentity read(InputStream inputStream) throws IOException {
        Ini ini = new Ini(inputStream);

        String identityValue = ini.get("Identity", "identity");
        if (identityValue == null)
            throw new IOException(new IllegalArgumentException("missing identity value"));

        Matcher matcher = identityPattern.matcher(identityValue);
        if (!matcher.matches())
            throw new IOException(new IllegalArgumentException("invalid INI file"));

        long level = Long.parseUnsignedLong(matcher.group(1));

        //var ident = Base64Decode(match.Groups["identity"].Value);
        byte[] identityData = Base64.getDecoder().decode(matcher.group(2));

        if (identityData.length < 20) throw new IOException(new IllegalArgumentException("identity data too short"));

        //int nullIdx = identityArr.AsSpan(20).IndexOf((byte)0);
        int nullIndex = -1;
        for (int i = 20; i < identityData.length; i ++)
            if (identityData[i] == 0x0) {
                nullIndex = i-20;
                break;
            }

        //var hash = Hash1It(identityArr, 20, nullIdx < 0 ? identityArr.Length - 20 : nullIdx);
        byte[] hash = Ts3Crypt.hash128(
                identityData,
                20,
                nullIndex < 0 ? identityData.length - 20 : nullIndex
        );

        //XorBinary(ReadOnlySpan<byte> a, ReadOnlySpan<byte> b, int len, Span<byte> outBuf)
        //XorBinary(identityArr, hash, 20, identityArr);
        Ts3Crypt.xor(
                identityData, 0,
                hash, 0,
                20,
                identityData, 0
        );

        //XorBinary(identityArr, Ts3IdentityObfuscationKey, Math.Min(100, identityArr.Length), identityArr);
        Ts3Crypt.xor(
                identityData, 0,
                identityFileObfuscationKey, 0,
                Math.min(100, identityData.length),
                identityData, 0
        );

        String utf8Decoded = new String(identityData, Charset.forName("UTF8"));

        LocalIdentity identity = Ts3Crypt.loadIdentityFromAsn(Base64.getDecoder().decode(utf8Decoded));

        identity.setKeyOffset(level);
        identity.setLastCheckedKeyOffset(level);

        return identity;
    }
}
