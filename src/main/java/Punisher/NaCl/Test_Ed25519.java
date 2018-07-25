package Punisher.NaCl;
import java.security.SecureRandom;

public class Test_Ed25519
{

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void main(String[] args) throws Exception
    {
        System.out.println("Test_Ed25519");
        SecureRandom sr = new SecureRandom();
        
        int trueC = 0;

        for (int i = 0; i < 500; i++)
        {

            // byte[] Seed = new byte[]{0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0,
            // 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3};

            byte[] Seed = new byte[]{0x34, (byte) 0x9E, (byte) 0x74, (byte) 0x52, (byte) 0xFA, (byte) 0x01, (byte) 0x8A, (byte) 0xD0, (byte) 0xE1, (byte) 0xE7, (byte) 0xA6, (byte) 0x3A, (byte) 0x77,
                    (byte) 0xF9, (byte) 0xE7, (byte) 0x78, (byte) 0x29, (byte) 0x30, (byte) 0x48, (byte) 0xE2, (byte) 0x93, (byte) 0xDE, (byte) 0x58, (byte) 0x3C, (byte) 0xC6, (byte) 0x84,
                    (byte) 0xFF, (byte) 0x56, (byte) 0xC7, (byte) 0x4D, (byte) 0xF8, (byte) 0xA4};

            sr.nextBytes(Seed);
            
            //byte posNext = (byte)(sr.nextInt() & 0x1F);
            Boolean doCheck = sr.nextBoolean();

            byte[] pk = Ed25519.PublicKeyFromSeed(Seed);
            byte[] sk = Ed25519.ExpandedPrivateKeyFromSeed(Seed);
            byte[] sig = Ed25519.Sign(new byte[32], sk);
            byte[] sss = new byte[32];
            
            if(doCheck)
            {
                /*byte oldVal = sss[posNext];
                byte newVal = 0;
                do
                {
                    newVal = (byte)(sr.nextInt() & 0xFF);
                }
                while(oldVal == newVal);
                sss[posNext] = newVal;     */   
                
                sr.nextBytes(sss);
            }
            else
            {
                 
            }
            
            Boolean bb = Ed25519.Verify(sig, sss, pk);            

           // System.out.println("TEST: " + i);
           // System.out.println("bb : " + bb);
            if (!doCheck || (doCheck && !bb))
            {
                trueC++;
               // System.out.println("PK : " + bytesToHex(pk));
               // System.out.println("SK : " + bytesToHex(sk));
               // System.out.println("sig : " + bytesToHex(sig));
            }

        }
        
        System.out.println("True Count: " + trueC);

    
    }

}
