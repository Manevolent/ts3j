package com.github.manevolent.ts3j.identity;

import com.github.manevolent.ts3j.util.Ts3Crypt;
import com.github.manevolent.ts3j.util.Ts3Debugging;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.math.ec.ECPoint;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Identity {
    private static MessageDigest sha1;

    static {
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final ECPoint publicKey;
    private final Uid uid;
    private final String publicKeyString;

    private long keyOffset = 0L, lastCheckedKeyOffset = 0L;

    public Identity(ECPoint publicKey) {
        this.publicKey = publicKey;
        this.publicKeyString = Base64.getEncoder().encodeToString(Ts3Crypt.encodePublicKey(publicKey));
        this.uid = generateUid();
    }

    public byte[] toASN() throws IOException {
        return new DERSequence(
                new ASN1Encodable[] {
                        new DERBitString(0),
                        new ASN1Integer(32),
                        new ASN1Integer(getPublicKey().getXCoord().toBigInteger()),
                        new ASN1Integer(getPublicKey().getYCoord().toBigInteger())
                }
        ).getEncoded();
    }

    public ECPoint getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyString() {
        return publicKeyString;
    }

    public Uid getUid() {
        return uid;
    }

    public long getLastCheckedKeyOffset() {
        return lastCheckedKeyOffset;
    }

    public void setLastCheckedKeyOffset(long lastCheckedKeyOffset) {
        this.lastCheckedKeyOffset = lastCheckedKeyOffset;
    }

    public long getKeyOffset() {
        return keyOffset;
    }

    public void setKeyOffset(long keyOffset) {
        this.keyOffset = keyOffset;
    }

    public void improveSecurity(int target) {
        byte[] hashBuffer = new byte[getPublicKeyString().length() + 20];
        byte[] pubKeyBytes = getPublicKeyString().getBytes(Charset.forName("ASCII"));
        System.arraycopy(pubKeyBytes, 0, hashBuffer, 0, pubKeyBytes.length);


        setLastCheckedKeyOffset(Math.max(getKeyOffset(), getLastCheckedKeyOffset()));
        int best = getSecurityLevel(hashBuffer, pubKeyBytes.length, getKeyOffset());
        while (true)
        {
            if (best >= target) return;

            int curr = getSecurityLevel(hashBuffer, pubKeyBytes.length, lastCheckedKeyOffset);

            if (curr > best)
            {
                Ts3Debugging.debug("Improved identity security level: from " + best + " to " + curr
                        + " (" + keyOffset + ")");
                keyOffset = lastCheckedKeyOffset;
                best = curr;
            }
            lastCheckedKeyOffset ++;
        }
    }

    public int getSecurityLevel() {
        byte[] hashBuffer = new byte[getPublicKeyString().length() + 20];
        byte[] pubKeyBytes = getPublicKeyString().getBytes(Charset.forName("ASCII"));
        System.arraycopy(pubKeyBytes, 0, hashBuffer, 0, pubKeyBytes.length);

        return getSecurityLevel(hashBuffer, pubKeyBytes.length, getKeyOffset());
    }

    private static int getSecurityLevel(byte[] hashBuffer, int pubKeyLen, long offset) {
        byte[] numBuffer = new byte[20];
        int numLen = 0;
        do
        {
            numBuffer[numLen] = (byte)(48 + (offset % 10));
            offset /= 10;
            numLen ++;
        } while (offset > 0);

        for (int i = 0; i < numLen; i ++)
            hashBuffer[pubKeyLen + i] = numBuffer[numLen - (i + 1)];

        byte[] outHash;
        synchronized (sha1) {
            sha1.update(hashBuffer, 0, pubKeyLen + numLen);
            outHash = sha1.digest();
        }

        return getLeadingZeroBits(outHash);
    }

    private static int getLeadingZeroBits(byte[] data) {
        int curr = 0;
        int i ;
        for (i = 0; i < data.length; i ++) {
            if (data[i] == 0) curr += 8;
            else break;
        }
        if (i < data.length)
            for (int bit = 0; bit < 8; bit ++) {
                if ((data[i] & (1 << bit)) == 0) curr++;
                else break;
            }
        return curr;
    }

    private Uid generateUid() {
        synchronized (sha1) {
            return new Uid(sha1.digest(getPublicKeyString().getBytes(Charset.forName("ASCII"))));
        }
    }
}
