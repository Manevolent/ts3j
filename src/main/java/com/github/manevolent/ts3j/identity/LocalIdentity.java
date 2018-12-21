package com.github.manevolent.ts3j.identity;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import org.bouncycastle.asn1.*;
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
import java.util.HashMap;
import java.util.Map;
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

    @Override
    public byte[] toASN() throws IOException {
        return new DERSequence(
                new ASN1Encodable[] {
                        new DERBitString(128),
                        new ASN1Integer(32),
                        new ASN1Integer(getPublicKey().getXCoord().toBigInteger()),
                        new ASN1Integer(getPublicKey().getYCoord().toBigInteger()),
                        new ASN1Integer(getPrivateKey())
                }
        ).getEncoded();
    }

    /**
     * Saves this local identity to a file
     * @param file File to save to
     * @throws IOException
     */
    public void save(File file) throws IOException {
        save(file, null);
    }

    /**
     * Saves this local identity to a file
     * @param file File to save to
     * @param  properties Additional properties to supply in the INI
     * @throws IOException
     */
    public void save(File file, Map<String, String> properties) throws IOException {
        save(new FileOutputStream(file), properties);
    }

    /**
     * Saves this local identity to an output stream (i.e.,file)
     * @param outputStream OutputStream instance to save raw bytes to
     * @throws IOException
     */
    public void save(OutputStream outputStream) throws IOException {
        save(outputStream, null);
    }

    /**
     * Saves this local identity to an output stream (i.e.,file)
     * @param outputStream OutputStream instance to save raw bytes to
     * @param  properties Additional properties to supply in the INI
     * @throws IOException
     */
    public void save(OutputStream outputStream, Map<String, String> properties) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream)) {
            writer.write(export(properties));
        }
    }

    /**
     * Exports a local identity object to a Teamspeak3 client friendly format.
     * @return TS3 identity format
     */
    public String export() throws IOException {
        return export(null);
    }

    /**
     * Exports a local identity object to a Teamspeak3 client friendly format.
     * @param  properties Additional properties to supply in the INI
     * @return TS3 identity format
     */
    public String export(Map<String, String> properties) throws IOException {
        Map<String, String> iniFile = new HashMap<>();

        if (properties != null) {
            // id, nickname, phonetic_nickname, etc.
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                iniFile.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder identityDataBuilder = new StringBuilder();

        // Encode key offset
        identityDataBuilder.append(getKeyOffset()).append("V");

        // Generate data for TS3 identity
        byte[] identityData = toASN();

        // Encode to UTF8
        identityData = Base64.getEncoder().encodeToString(identityData).getBytes("UTF8");

        // Encrypt identity
        Ts3Crypt.xor(
                identityData, 0,
                identityFileObfuscationKey, 0,
                Math.min(100, identityData.length),
                identityData, 0
        );

        int nullIndex = 0x0; // STATIC

        byte[] hash = Ts3Crypt.hash128(
                identityData,
                20,
                nullIndex < 0 ? identityData.length - 20 : nullIndex
        );

        Ts3Crypt.xor(
                identityData, 0,
                hash, 0,
                20,
                identityData, 0
        );

        // Place down Base64 identity data for TS3 identity
        identityDataBuilder.append(Base64.getEncoder().encodeToString(identityData));

        iniFile.put("identity", identityDataBuilder.toString());

        StringBuilder builder = new StringBuilder();
        builder.append("[Identity]").append("\r\n");
        for (Map.Entry<String, String> entry : iniFile.entrySet()) {
            builder.append(entry.getKey())
                    .append("=")
                    .append("\"").append(entry.getValue().replace("\"", "\\\"")).append("\"")
                    .append("\r\n");
        }
        return builder.toString();
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
