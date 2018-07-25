package Punisher.NaCl.Internal;

import Punisher.NaCl.CryptoBytes;

public class Sha512
{
    private Array8<Long> _state = new Array8<Long>() ;
    private  byte[] _buffer;
    private long _totalBytes;
    public  int BlockSize = 128;
    private static  byte[] _padding = new byte[] { (byte)0x80 };

    public Sha512()
    {
        _buffer = new byte[BlockSize];
        Init();
    }

    public void Init()
    {
        Sha512Internal.Sha512Init(_state);
        _totalBytes = 0;
    }   

    public void Update(byte[] data, int offset, int count) throws Exception 
    {
        if (data == null)
            throw new Exception("data");
        if (offset < 0)
            throw new Exception("offset");
        if (count < 0)
            throw new Exception("count");
        if (data.length - offset < count)
            throw new Exception("Requires offset + count <= data.Length");

        Array16<Long> block = new Array16<Long>();
        int bytesInBuffer = (int)_totalBytes & (BlockSize - 1);
        _totalBytes += (int)count;

        if (_totalBytes >= Long.MAX_VALUE / 8)
            throw new Exception("Too much data");
        // Fill existing buffer
        if (bytesInBuffer != 0)
        {
            int toCopy = Math.min(BlockSize - bytesInBuffer, count);
            System.arraycopy(data, offset, _buffer, bytesInBuffer, toCopy);
            offset += toCopy;
            count -= toCopy;
            bytesInBuffer += toCopy;
            if (bytesInBuffer == BlockSize)
            {
                ByteIntegerConverter.Array16LoadBigEndian64(block, _buffer, 0);
                Sha512Internal.Core(_state, _state, block);
                CryptoBytes.Wipe(_buffer);
                bytesInBuffer = 0;
            }
        }
        // Hash complete blocks without copying
        while (count >= BlockSize)
        {
            ByteIntegerConverter.Array16LoadBigEndian64(block, data, offset);
            Sha512Internal.Core(_state, _state,  block);
            offset += BlockSize;
            count -= BlockSize;
        }
        // Copy remainder into buffer
        if (count > 0)
        {
            System.arraycopy(data, offset, _buffer, bytesInBuffer, count);
        }
    }
    
    public void Finish(byte [] output) throws Exception 
    {
        if (output == null)
            throw new Exception("output.Array");
        if (output.length != 64)
            throw new Exception("output.Count must be 64");

        Update(_padding, 0, _padding.length);
        Array16<Long> block = new Array16<Long>();
        ByteIntegerConverter.Array16LoadBigEndian64(block, _buffer, 0);
        CryptoBytes.Wipe(_buffer);
        int bytesInBuffer = (int)_totalBytes & (BlockSize - 1);
        if (bytesInBuffer > BlockSize - 16)
        {
            Sha512Internal.Core(_state,  _state,  block);
            block = new Array16<Long>();
        }
        block.x15 = (_totalBytes - 1) * 8;
        Sha512Internal.Core(_state, _state, block);

        ByteIntegerConverter.StoreBigEndian64(output,  0, _state.x0);
        ByteIntegerConverter.StoreBigEndian64(output,  8, _state.x1);
        ByteIntegerConverter.StoreBigEndian64(output,  16, _state.x2);
        ByteIntegerConverter.StoreBigEndian64(output,  24, _state.x3);
        ByteIntegerConverter.StoreBigEndian64(output,  32, _state.x4);
        ByteIntegerConverter.StoreBigEndian64(output,  40, _state.x5);
        ByteIntegerConverter.StoreBigEndian64(output,  48, _state.x6);
        ByteIntegerConverter.StoreBigEndian64(output,  56, _state.x7);
        _state = new Array8<Long>();
    }

    public byte[] Finish() throws Exception
    {
        byte[] result = new byte[64];
        Finish(result);
        return result;
    }

    public static byte[] Hash(byte[] data) throws Exception
    {
        return Hash(data, 0, data.length);
    }

    public static byte[] Hash(byte[] data, int offset, int count) throws Exception
    {
        Sha512 hasher = new Sha512();
        hasher.Update(data, offset, count);
        return hasher.Finish();
    }
}