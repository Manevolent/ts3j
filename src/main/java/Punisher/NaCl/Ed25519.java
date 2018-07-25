package Punisher.NaCl;

public class Ed25519
{
    public static  int PublicKeySizeInBytes = 32;
    public static  int SignatureSizeInBytes = 64;
    public static  int ExpandedPrivateKeySizeInBytes = 32 * 2;
    public static  int PrivateKeySeedSizeInBytes = 32;
    public static  int SharedKeySizeInBytes = 32;

    public static Boolean Verify(byte[] signature, byte[] message, byte[] publicKey) throws Exception
    {
        if (signature == null)
            throw new Exception("signature");
        if (message == null)
            throw new Exception("message");
        if (publicKey == null)
            throw new Exception("publicKey");
        if (signature.length != SignatureSizeInBytes)
            throw new Exception("Signature size must be " + SignatureSizeInBytes);
        if (publicKey.length != PublicKeySizeInBytes)
            throw new Exception("Public key size must be "+  PublicKeySizeInBytes);
        return Ed25519Operations.crypto_sign_verify(signature, 0, message, 0, message.length, publicKey, 0);
    }

    public static void Sign(byte[] signature, byte[] message, byte[] expandedPrivateKey) throws Exception
    {
        if (signature == null)
            throw new Exception("signature.Array");
        if (signature.length != SignatureSizeInBytes)
            throw new Exception("signature.Count");
        if (expandedPrivateKey == null)
            throw new Exception("expandedPrivateKey.Array");
        if (expandedPrivateKey.length != ExpandedPrivateKeySizeInBytes)
            throw new Exception("expandedPrivateKey.Count");
        if (message == null)
            throw new Exception("message.Array");
        
        Ed25519Operations.crypto_sign2(signature,0, message, 0, message.length, expandedPrivateKey, 0);
    }

    public static byte[] Sign(byte[] message, byte[] expandedPrivateKey) throws Exception
    {
        byte[] signature = new byte[SignatureSizeInBytes];
        Sign(signature,message,expandedPrivateKey);
        return signature;
    }

    public static byte[] PublicKeyFromSeed(byte[] privateKeySeed) throws Exception
    {
        byte[] privateKey = new byte[ExpandedPrivateKeySizeInBytes];
        byte[] publicKey = new byte[PublicKeySizeInBytes];
        KeyPairFromSeed( publicKey,  privateKey, privateKeySeed);
        CryptoBytes.Wipe(privateKey);
        return publicKey;
    }

    public static byte[] ExpandedPrivateKeyFromSeed(byte[] privateKeySeed) throws Exception
    {
        byte[] privateKey = new byte[ExpandedPrivateKeySizeInBytes];
        byte[] publicKey = new byte[PublicKeySizeInBytes];
        KeyPairFromSeed( publicKey,  privateKey, privateKeySeed);
        CryptoBytes.Wipe(publicKey);
        return privateKey;
    }

    public static void KeyPairFromSeed( byte[] publicKey,  byte[] expandedPrivateKey, byte[] privateKeySeed) throws Exception
    {
        if (privateKeySeed == null)
            throw new Exception("privateKeySeed");
        if (privateKeySeed.length != PrivateKeySeedSizeInBytes)
            throw new Exception("privateKeySeed");        
        if (expandedPrivateKey == null)
            throw new Exception("expandedPrivateKey");
        if (expandedPrivateKey.length != ExpandedPrivateKeySizeInBytes)
            throw new Exception("expandedPrivateKey");        
        if (publicKey == null)
            throw new Exception("publicKey");
        if (publicKey.length != PublicKeySizeInBytes)
            throw new Exception("publicKey");

        Ed25519Operations.crypto_sign_keypair(publicKey, 0, expandedPrivateKey, 0, privateKeySeed, 0);   
    }
    
}
