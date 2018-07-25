package Punisher.NaCl;

public class CryptoBytes
{
    public static Boolean ConstantTimeEquals(byte[] x, int xOffset, byte[] y, int yOffset, int length) throws Exception
    {
        if (x == null) throw new Exception("x");
        if (xOffset < 0) throw new Exception("xOffset" + "xOffset < 0");
        if (y == null) throw new Exception("y");
        if (yOffset < 0) throw new Exception("yOffset" + "yOffset < 0");
        if (length < 0) throw new Exception("length" + "length < 0");
        if (x.length - xOffset < length) throw new Exception("xOffset + length > x.Length");
        if (y.length - yOffset < length) throw new Exception("yOffset + length > y.Length");

        return InternalConstantTimeEquals(x, xOffset, y, yOffset, length);
    }

    public static boolean InternalConstantTimeEquals(byte[] x, int xOffset, byte[] y, int yOffset, int length)
    {
        int result = 0;
        for (int i = 0; i < length; i++)
        {
            result |= x[xOffset + i] ^ y[yOffset + i];
        }
        return result == 0; // Check const time
    }

    public static void Wipe(byte[] data)
    {
        for (int i = 0; i < data.length; i++)
            data[i] = 0;
    }

}
