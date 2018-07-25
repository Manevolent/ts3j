package Punisher.NaCl;

import Punisher.NaCl.Internal.Sha512;
import Punisher.NaCl.Internal.Ed25519Ref10.*;

public class Ed25519Operations
{
    public static Boolean crypto_sign_verify(byte[] sig, int sigoffset, byte[] m, int moffset, int mlen, byte[] pk, int pkoffset) throws Exception
    {
        byte[] h;
        byte[] checkr = new byte[32];
        GroupElementP3 A = new GroupElementP3();
        GroupElementP2 R = new GroupElementP2();

        if ((sig[sigoffset + 63] & 224) != 0) return false;
        if (GroupOperations.ge_frombytes_negate_vartime(A, pk, pkoffset) != 0) return false;

        Sha512 hasher = new Sha512();
        hasher.Update(sig, sigoffset, 32);
        hasher.Update(pk, pkoffset, 32);
        hasher.Update(m, moffset, mlen);
        h = hasher.Finish();

        ScalarOperations.sc_reduce(h);

        byte[] sm32 = new byte[32];// todo: remove allocation
        System.arraycopy(sig, sigoffset + 32, sm32, 0, 32);
        GroupOperations.ge_double_scalarmult_vartime(R, h, A, sm32);
        GroupOperations.ge_tobytes(checkr, 0, R);
        Boolean result = CryptoBytes.ConstantTimeEquals(checkr, 0, sig, sigoffset, 32);
        CryptoBytes.Wipe(h);
        CryptoBytes.Wipe(checkr);
        return result;
    }

    public static void crypto_sign2(byte[] sig, int sigoffset, byte[] m, int moffset, int mlen, byte[] sk, int skoffset) throws Exception
    {
        byte[] az;
        byte[] r;
        byte[] hram;
        GroupElementP3 R = new GroupElementP3();
        Sha512 hasher = new Sha512();
        {
            hasher.Update(sk, skoffset, 32);
            az = hasher.Finish();
            ScalarOperations.sc_clamp(az, 0);

            hasher.Init();
            hasher.Update(az, 32, 32);
            hasher.Update(m, moffset, mlen);
            r = hasher.Finish();

            ScalarOperations.sc_reduce(r);
            GroupOperations.ge_scalarmult_base(R, r, 0);
            GroupOperations.ge_p3_tobytes(sig, sigoffset, R);

            hasher.Init();
            hasher.Update(sig, sigoffset, 32);
            hasher.Update(sk, skoffset + 32, 32);
            hasher.Update(m, moffset, mlen);
            hram = hasher.Finish();

            ScalarOperations.sc_reduce(hram);
            byte[] s = new byte[32];// todo: remove allocation
            System.arraycopy(sig, sigoffset + 32, s, 0, 32);
            ScalarOperations.sc_muladd(s, hram, az, r);
            System.arraycopy(s, 0, sig, sigoffset + 32, 32);
            CryptoBytes.Wipe(s);
        }
    }

    public static void crypto_sign_keypair(byte[] pk, int pkoffset, byte[] sk, int skoffset, byte[] seed, int seedoffset) throws Exception
    {
        GroupElementP3 A = new GroupElementP3();
        int i;

        System.arraycopy(seed, seedoffset, sk, skoffset, 32);
        byte[] h = Sha512.Hash(sk, skoffset, 32); // ToDo: Remove alloc
        ScalarOperations.sc_clamp(h, 0);

        GroupOperations.ge_scalarmult_base(A, h, 0);
        GroupOperations.ge_p3_tobytes(pk, pkoffset, A);

        for (i = 0; i < 32; ++i)
            sk[skoffset + 32 + i] = pk[pkoffset + i];
        
        CryptoBytes.Wipe(h);
    }

}
