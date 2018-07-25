package Punisher.NaCl.Internal;

public class Sha512Internal
{
    private static final long[] K = new long[]{0x428a2f98d728ae22L, 0x7137449123ef65cdL, 0xb5c0fbcfec4d3b2fL, 0xe9b5dba58189dbbcL, 0x3956c25bf348b538L, 0x59f111f1b605d019L, 0x923f82a4af194f9bL,
            0xab1c5ed5da6d8118L, 0xd807aa98a3030242L, 0x12835b0145706fbeL, 0x243185be4ee4b28cL, 0x550c7dc3d5ffb4e2L, 0x72be5d74f27b896fL, 0x80deb1fe3b1696b1L, 0x9bdc06a725c71235L,
            0xc19bf174cf692694L, 0xe49b69c19ef14ad2L, 0xefbe4786384f25e3L, 0x0fc19dc68b8cd5b5L, 0x240ca1cc77ac9c65L, 0x2de92c6f592b0275L, 0x4a7484aa6ea6e483L, 0x5cb0a9dcbd41fbd4L,
            0x76f988da831153b5L, 0x983e5152ee66dfabL, 0xa831c66d2db43210L, 0xb00327c898fb213fL, 0xbf597fc7beef0ee4L, 0xc6e00bf33da88fc2L, 0xd5a79147930aa725L, 0x06ca6351e003826fL,
            0x142929670a0e6e70L, 0x27b70a8546d22ffcL, 0x2e1b21385c26c926L, 0x4d2c6dfc5ac42aedL, 0x53380d139d95b3dfL, 0x650a73548baf63deL, 0x766a0abb3c77b2a8L, 0x81c2c92e47edaee6L,
            0x92722c851482353bL, 0xa2bfe8a14cf10364L, 0xa81a664bbc423001L, 0xc24b8b70d0f89791L, 0xc76c51a30654be30L, 0xd192e819d6ef5218L, 0xd69906245565a910L, 0xf40e35855771202aL,
            0x106aa07032bbd1b8L, 0x19a4c116b8d2d0c8L, 0x1e376c085141ab53L, 0x2748774cdf8eeb99L, 0x34b0bcb5e19b48a8L, 0x391c0cb3c5c95a63L, 0x4ed8aa4ae3418acbL, 0x5b9cca4f7763e373L,
            0x682e6ff3d6b2b8a3L, 0x748f82ee5defb2fcL, 0x78a5636f43172f60L, 0x84c87814a1f0ab72L, 0x8cc702081a6439ecL, 0x90befffa23631e28L, 0xa4506cebde82bde9L, 0xbef9a3f7b2c67915L,
            0xc67178f2e372532bL, 0xca273eceea26619cL, 0xd186b8c721c0c207L, 0xeada7dd6cde0eb1eL, 0xf57d4f7fee6ed178L, 0x06f067aa72176fbaL, 0x0a637dc5a2c898a6L, 0x113f9804bef90daeL,
            0x1b710b35131c471bL, 0x28db77f523047d84L, 0x32caab7b40c72493L, 0x3c9ebe0a15c9bebcL, 0x431d67c49c100d4cL, 0x4cc5d4becb3e42b6L, 0x597f299cfc657e2aL, 0x5fcb6fab3ad6faecL, 0x6c44198c4a475817L};

    public static void Sha512Init(Array8<Long> state)
    {
        state.x0 = 0x6a09e667f3bcc908L;
        state.x1 = 0xbb67ae8584caa73bL;
        state.x2 = 0x3c6ef372fe94f82bL;
        state.x3 = 0xa54ff53a5f1d36f1L;
        state.x4 = 0x510e527fade682d1L;
        state.x5 = 0x9b05688c2b3e6c1fL;
        state.x6 = 0x1f83d9abfb41bd6bL;
        state.x7 = 0x5be0cd19137e2179L;
    }

    public static void Core(Array8<Long> outputState, Array8<Long> inputState, Array16<Long> input)
    {

        Long a = inputState.x0;
        Long b = inputState.x1;
        Long c = inputState.x2;
        Long d = inputState.x3;
        Long e = inputState.x4;
        Long f = inputState.x5;
        Long g = inputState.x6;
        Long h = inputState.x7;

        Long w0 = input.x0;
        Long w1 = input.x1;
        Long w2 = input.x2;
        Long w3 = input.x3;
        Long w4 = input.x4;
        Long w5 = input.x5;
        Long w6 = input.x6;
        Long w7 = input.x7;
        Long w8 = input.x8;
        Long w9 = input.x9;
        Long w10 = input.x10;
        Long w11 = input.x11;
        Long w12 = input.x12;
        Long w13 = input.x13;
        Long w14 = input.x14;
        Long w15 = input.x15;

        int t = 0;
        while (true)
        {
            Long t1, t2;

            {// 0
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w0;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 1
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w1;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 2
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w2;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 3
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w3;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 4
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w4;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 5
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w5;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 6
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w6;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 7
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w7;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 8
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w8;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 9
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w9;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 10
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w10;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 11
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w11;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 12
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w12;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 13
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w13;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 14
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w14;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            {// 15
                t1 = h + ((e >>> 14) ^ (e << (64 - 14)) ^ (e >>> 18) ^ (e << (64 - 18)) ^ (e >>> 41) ^ (e << (64 - 41))) +
                // Sigma1(e)
                        ((e & f) ^ (~e & g)) + // Ch(e,f,g)
                        K[t] + w15;
                t2 = ((a >>> 28) ^ (a << (64 - 28)) ^ (a >>> 34) ^ (a << (64 - 34)) ^ (a >>> 39) ^ (a << (64 - 39))) +
                // Sigma0(a)
                        ((a & b) ^ (a & c) ^ (b & c)); // Maj(a,b,c)
                h = g;
                g = f;
                f = e;
                e = d + t1;
                d = c;
                c = b;
                b = a;
                a = t1 + t2;
                t++;
            }
            if (t == 80) break;

            w0 += ((w14 >>> 19) ^ (w14 << (64 - 19)) ^ (w14 >>> 61) ^ (w14 << (64 - 61)) ^ (w14 >>> 6)) + w9 + ((w1 >>> 1) ^ (w1 << (64 - 1)) ^ (w1 >>> 8) ^ (w1 << (64 - 8)) ^ (w1 >>> 7));
            w1 += ((w15 >>> 19) ^ (w15 << (64 - 19)) ^ (w15 >>> 61) ^ (w15 << (64 - 61)) ^ (w15 >>> 6)) + w10 + ((w2 >>> 1) ^ (w2 << (64 - 1)) ^ (w2 >>> 8) ^ (w2 << (64 - 8)) ^ (w2 >>> 7));
            w2 += ((w0 >>> 19) ^ (w0 << (64 - 19)) ^ (w0 >>> 61) ^ (w0 << (64 - 61)) ^ (w0 >>> 6)) + w11 + ((w3 >>> 1) ^ (w3 << (64 - 1)) ^ (w3 >>> 8) ^ (w3 << (64 - 8)) ^ (w3 >>> 7));
            w3 += ((w1 >>> 19) ^ (w1 << (64 - 19)) ^ (w1 >>> 61) ^ (w1 << (64 - 61)) ^ (w1 >>> 6)) + w12 + ((w4 >>> 1) ^ (w4 << (64 - 1)) ^ (w4 >>> 8) ^ (w4 << (64 - 8)) ^ (w4 >>> 7));
            w4 += ((w2 >>> 19) ^ (w2 << (64 - 19)) ^ (w2 >>> 61) ^ (w2 << (64 - 61)) ^ (w2 >>> 6)) + w13 + ((w5 >>> 1) ^ (w5 << (64 - 1)) ^ (w5 >>> 8) ^ (w5 << (64 - 8)) ^ (w5 >>> 7));
            w5 += ((w3 >>> 19) ^ (w3 << (64 - 19)) ^ (w3 >>> 61) ^ (w3 << (64 - 61)) ^ (w3 >>> 6)) + w14 + ((w6 >>> 1) ^ (w6 << (64 - 1)) ^ (w6 >>> 8) ^ (w6 << (64 - 8)) ^ (w6 >>> 7));
            w6 += ((w4 >>> 19) ^ (w4 << (64 - 19)) ^ (w4 >>> 61) ^ (w4 << (64 - 61)) ^ (w4 >>> 6)) + w15 + ((w7 >>> 1) ^ (w7 << (64 - 1)) ^ (w7 >>> 8) ^ (w7 << (64 - 8)) ^ (w7 >>> 7));
            w7 += ((w5 >>> 19) ^ (w5 << (64 - 19)) ^ (w5 >>> 61) ^ (w5 << (64 - 61)) ^ (w5 >>> 6)) + w0 + ((w8 >>> 1) ^ (w8 << (64 - 1)) ^ (w8 >>> 8) ^ (w8 << (64 - 8)) ^ (w8 >>> 7));
            w8 += ((w6 >>> 19) ^ (w6 << (64 - 19)) ^ (w6 >>> 61) ^ (w6 << (64 - 61)) ^ (w6 >>> 6)) + w1 + ((w9 >>> 1) ^ (w9 << (64 - 1)) ^ (w9 >>> 8) ^ (w9 << (64 - 8)) ^ (w9 >>> 7));
            w9 += ((w7 >>> 19) ^ (w7 << (64 - 19)) ^ (w7 >>> 61) ^ (w7 << (64 - 61)) ^ (w7 >>> 6)) + w2 + ((w10 >>> 1) ^ (w10 << (64 - 1)) ^ (w10 >>> 8) ^ (w10 << (64 - 8)) ^ (w10 >>> 7));
            w10 += ((w8 >>> 19) ^ (w8 << (64 - 19)) ^ (w8 >>> 61) ^ (w8 << (64 - 61)) ^ (w8 >>> 6)) + w3 + ((w11 >>> 1) ^ (w11 << (64 - 1)) ^ (w11 >>> 8) ^ (w11 << (64 - 8)) ^ (w11 >>> 7));
            w11 += ((w9 >>> 19) ^ (w9 << (64 - 19)) ^ (w9 >>> 61) ^ (w9 << (64 - 61)) ^ (w9 >>> 6)) + w4 + ((w12 >>> 1) ^ (w12 << (64 - 1)) ^ (w12 >>> 8) ^ (w12 << (64 - 8)) ^ (w12 >>> 7));
            w12 += ((w10 >>> 19) ^ (w10 << (64 - 19)) ^ (w10 >>> 61) ^ (w10 << (64 - 61)) ^ (w10 >>> 6)) + w5 + ((w13 >>> 1) ^ (w13 << (64 - 1)) ^ (w13 >>> 8) ^ (w13 << (64 - 8)) ^ (w13 >>> 7));
            w13 += ((w11 >>> 19) ^ (w11 << (64 - 19)) ^ (w11 >>> 61) ^ (w11 << (64 - 61)) ^ (w11 >>> 6)) + w6 + ((w14 >>> 1) ^ (w14 << (64 - 1)) ^ (w14 >>> 8) ^ (w14 << (64 - 8)) ^ (w14 >>> 7));
            w14 += ((w12 >>> 19) ^ (w12 << (64 - 19)) ^ (w12 >>> 61) ^ (w12 << (64 - 61)) ^ (w12 >>> 6)) + w7 + ((w15 >>> 1) ^ (w15 << (64 - 1)) ^ (w15 >>> 8) ^ (w15 << (64 - 8)) ^ (w15 >>> 7));
            w15 += ((w13 >>> 19) ^ (w13 << (64 - 19)) ^ (w13 >>> 61) ^ (w13 << (64 - 61)) ^ (w13 >>> 6)) + w8 + ((w0 >>> 1) ^ (w0 << (64 - 1)) ^ (w0 >>> 8) ^ (w0 << (64 - 8)) ^ (w0 >>> 7));
        }

        outputState.x0 = inputState.x0 + a;
        outputState.x1 = inputState.x1 + b;
        outputState.x2 = inputState.x2 + c;
        outputState.x3 = inputState.x3 + d;
        outputState.x4 = inputState.x4 + e;
        outputState.x5 = inputState.x5 + f;
        outputState.x6 = inputState.x6 + g;
        outputState.x7 = inputState.x7 + h;
    }

}