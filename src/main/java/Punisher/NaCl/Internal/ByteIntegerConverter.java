package Punisher.NaCl.Internal;

public class ByteIntegerConverter
{    
    public static int LoadLittleEndian32(byte[] buf, int offset)
    {
        return
            (int)(buf[offset + 0]& 0xFF)
        | (((int)(buf[offset + 1])& 0xFF) << 8)
        | (((int)(buf[offset + 2])& 0xFF) << 16)
        | (((int)(buf[offset + 3])& 0xFF) << 24);
    }

    public static void StoreLittleEndian32(byte[] buf, int offset, int value)
    {
        buf[offset + 0] = ((byte)value);
        buf[offset + 1] = ((byte)(value >>> 8));
        buf[offset + 2] = ((byte)(value >>> 16));
        buf[offset + 3] = ((byte)(value >>> 24));
    }

    public static long LoadBigEndian64(byte[] buf, int offset)
    {
        return  (long)(buf[offset + 7] & 0xFF)
                | (((long)(buf[offset + 6]) & 0xFF) << 8)
                | (((long)(buf[offset + 5]) & 0xFF) << 16)
                | (((long)(buf[offset + 4]) & 0xFF) << 24)
                | (((long)(buf[offset + 3]) & 0xFF) << 32)
                | (((long)(buf[offset + 2]) & 0xFF) << 40)
                | (((long)(buf[offset + 1]) & 0xFF) << 48)
                | (((long)(buf[offset + 0]) & 0xFF) << 56);
        
        //return N;           
    }

    public static void StoreBigEndian64(byte[] buf, int offset, long value)
    {
        buf[offset + 7] = ((byte)value);
        buf[offset + 6] = ((byte)(value >>> 8));
        buf[offset + 5] = ((byte)(value >>> 16));
        buf[offset + 4] = ((byte)(value >>> 24));
        buf[offset + 3] = ((byte)(value >>> 32));
        buf[offset + 2] = ((byte)(value >>> 40));
        buf[offset + 1] = ((byte)(value >>> 48));
        buf[offset + 0] = ((byte)(value >>> 56));
    }

    /*public static void XorLittleEndian32(byte[] buf, int offset, UInt32 value)
    {
        buf[offset + 0] ^= (byte)value;
        buf[offset + 1] ^= (byte)(value >> 8);
        buf[offset + 2] ^= (byte)(value >> 16);
        buf[offset + 3] ^= (byte)(value >> 24);
    }*/

    /*public static void XorLittleEndian32(byte[] output, int outputOffset, byte[] input, int inputOffset, UInt32 value)
    {
        output[outputOffset + 0] = (byte)(input[inputOffset + 0] ^ value);
        output[outputOffset + 1] = (byte)(input[inputOffset + 1] ^ (value >> 8));
        output[outputOffset + 2] = (byte)(input[inputOffset + 2] ^ (value >> 16));
        output[outputOffset + 3] = (byte)(input[inputOffset + 3] ^ (value >> 24));
    }*/

 

    public static void Array8LoadLittleEndian32( Array8<Integer> output, byte[] input, int inputOffset)
    {
        output.x0 = LoadLittleEndian32(input, inputOffset + 0);
        output.x1 = LoadLittleEndian32(input, inputOffset + 4);
        output.x2 = LoadLittleEndian32(input, inputOffset + 8);
        output.x3 = LoadLittleEndian32(input, inputOffset + 12);
        output.x4 = LoadLittleEndian32(input, inputOffset + 16);
        output.x5 = LoadLittleEndian32(input, inputOffset + 20);
        output.x6 = LoadLittleEndian32(input, inputOffset + 24);
        output.x7 = LoadLittleEndian32(input, inputOffset + 28);
    }


    public static void Array16LoadBigEndian64( Array16<Long> output, byte[] input, int inputOffset)
    {
        output.x0 = LoadBigEndian64(input, inputOffset + 0);
        output.x1 = LoadBigEndian64(input, inputOffset + 8);
        output.x2 = LoadBigEndian64(input, inputOffset + 16);
        output.x3 = LoadBigEndian64(input, inputOffset + 24);
        output.x4 = LoadBigEndian64(input, inputOffset + 32);
        output.x5 = LoadBigEndian64(input, inputOffset + 40);
        output.x6 = LoadBigEndian64(input, inputOffset + 48);
        output.x7 = LoadBigEndian64(input, inputOffset + 56);
        output.x8 = LoadBigEndian64(input, inputOffset + 64);
        output.x9 = LoadBigEndian64(input, inputOffset + 72);
        output.x10 = LoadBigEndian64(input, inputOffset + 80);
        output.x11 = LoadBigEndian64(input, inputOffset + 88);
        output.x12 = LoadBigEndian64(input, inputOffset + 96);
        output.x13 = LoadBigEndian64(input, inputOffset + 104);
        output.x14 = LoadBigEndian64(input, inputOffset + 112);
        output.x15 = LoadBigEndian64(input, inputOffset + 120);
    }

    // ToDo: Only used in tests. Remove?
    public static void Array16LoadLittleEndian32( Array16<Integer> output, byte[] input, int inputOffset)
    {
        output.x0 = LoadLittleEndian32(input, inputOffset + 0);
        output.x1 = LoadLittleEndian32(input, inputOffset + 4);
        output.x2 = LoadLittleEndian32(input, inputOffset + 8);
        output.x3 = LoadLittleEndian32(input, inputOffset + 12);
        output.x4 = LoadLittleEndian32(input, inputOffset + 16);
        output.x5 = LoadLittleEndian32(input, inputOffset + 20);
        output.x6 = LoadLittleEndian32(input, inputOffset + 24);
        output.x7 = LoadLittleEndian32(input, inputOffset + 28);
        output.x8 = LoadLittleEndian32(input, inputOffset + 32);
        output.x9 = LoadLittleEndian32(input, inputOffset + 36);
        output.x10 = LoadLittleEndian32(input, inputOffset + 40);
        output.x11 = LoadLittleEndian32(input, inputOffset + 44);
        output.x12 = LoadLittleEndian32(input, inputOffset + 48);
        output.x13 = LoadLittleEndian32(input, inputOffset + 52);
        output.x14 = LoadLittleEndian32(input, inputOffset + 56);
        output.x15 = LoadLittleEndian32(input, inputOffset + 60);
    }

    /*public static void Array16LoadLittleEndian32(out Array16<UInt32> output, byte[] input, int inputOffset, int inputLength)
    {
        Array8<UInt32> temp;
        if (inputLength > 32)
        {
            output.x0 = LoadLittleEndian32(input, inputOffset + 0);
            output.x1 = LoadLittleEndian32(input, inputOffset + 4);
            output.x2 = LoadLittleEndian32(input, inputOffset + 8);
            output.x3 = LoadLittleEndian32(input, inputOffset + 12);
            output.x4 = LoadLittleEndian32(input, inputOffset + 16);
            output.x5 = LoadLittleEndian32(input, inputOffset + 20);
            output.x6 = LoadLittleEndian32(input, inputOffset + 24);
            output.x7 = LoadLittleEndian32(input, inputOffset + 28);
            Array8LoadLittleEndian32(out temp, input, inputOffset + 32, inputLength - 32);
            output.x8 = temp.x0;
            output.x9 = temp.x1;
            output.x10 = temp.x2;
            output.x11 = temp.x3;
            output.x12 = temp.x4;
            output.x13 = temp.x5;
            output.x14 = temp.x6;
            output.x15 = temp.x7;
        }
        else
        {
            Array8LoadLittleEndian32(out temp, input, inputOffset, inputLength);
            output.x0 = temp.x0;
            output.x1 = temp.x1;
            output.x2 = temp.x2;
            output.x3 = temp.x3;
            output.x4 = temp.x4;
            output.x5 = temp.x5;
            output.x6 = temp.x6;
            output.x7 = temp.x7;
            output.x8 = 0;
            output.x9 = 0;
            output.x10 = 0;
            output.x11 = 0;
            output.x12 = 0;
            output.x13 = 0;
            output.x14 = 0;
            output.x15 = 0;
        }
    }*/

    public static void Array16StoreLittleEndian32(byte[] output, int outputOffset, Array16<Integer> input)
    {
        StoreLittleEndian32(output, outputOffset + 0, input.x0);
        StoreLittleEndian32(output, outputOffset + 4, input.x1);
        StoreLittleEndian32(output, outputOffset + 8, input.x2);
        StoreLittleEndian32(output, outputOffset + 12, input.x3);
        StoreLittleEndian32(output, outputOffset + 16, input.x4);
        StoreLittleEndian32(output, outputOffset + 20, input.x5);
        StoreLittleEndian32(output, outputOffset + 24, input.x6);
        StoreLittleEndian32(output, outputOffset + 28, input.x7);
        StoreLittleEndian32(output, outputOffset + 32, input.x8);
        StoreLittleEndian32(output, outputOffset + 36, input.x9);
        StoreLittleEndian32(output, outputOffset + 40, input.x10);
        StoreLittleEndian32(output, outputOffset + 44, input.x11);
        StoreLittleEndian32(output, outputOffset + 48, input.x12);
        StoreLittleEndian32(output, outputOffset + 52, input.x13);
        StoreLittleEndian32(output, outputOffset + 56, input.x14);
        StoreLittleEndian32(output, outputOffset + 60, input.x15);
    }
}
