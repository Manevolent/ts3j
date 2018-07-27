package Punisher.NaCl.Internal.Ed25519Ref10;

public class GroupOperations
{
    public static int ge_frombytes_negate_vartime(GroupElementP3 h, byte[] data, int offset)
    {
        FieldElement u = new FieldElement();
        FieldElement v = new FieldElement();
        FieldElement v3 = new FieldElement();
        FieldElement vxx = new FieldElement();
        FieldElement check = new FieldElement();

        FieldOperations.fe_frombytes(h.Y, data, offset);
        FieldOperations.fe_1(h.Z);
        FieldOperations.fe_sq(u, h.Y);
        FieldOperations.fe_mul(v, u, LookupTables.d);
        FieldOperations.fe_sub(u, u, h.Z); /* u = y^2-1 */
        FieldOperations.fe_add(v, v, h.Z); /* v = dy^2+1 */

        FieldOperations.fe_sq(v3, v);
        FieldOperations.fe_mul(v3, v3, v); /* v3 = v^3 */
        FieldOperations.fe_sq(h.X, v3);
        FieldOperations.fe_mul(h.X, h.X, v);
        FieldOperations.fe_mul(h.X, h.X, u); /* x = uv^7 */

        FieldOperations.fe_pow22523(h.X, h.X); /* x = (uv^7)^((q-5)/8) */
        FieldOperations.fe_mul(h.X, h.X, v3);
        FieldOperations.fe_mul(h.X, h.X, u); /* x = uv^3(uv^7)^((q-5)/8) */

        FieldOperations.fe_sq(vxx, h.X);
        FieldOperations.fe_mul(vxx, vxx, v);
        FieldOperations.fe_sub(check, vxx, u); /* vx^2-u */
        if (FieldOperations.fe_isnonzero(check) != 0)
        {
            FieldOperations.fe_add(check, vxx, u); /* vx^2+u */
            if (FieldOperations.fe_isnonzero(check) != 0)
            {
                h.ReInit();// = new GroupElementP3();
                return -1;
            }
            FieldOperations.fe_mul(h.X, h.X, LookupTables.sqrtm1);
        }

        if (FieldOperations.fe_isnegative(h.X) == ((data[offset + 31] & 0xFF) >> 7)) FieldOperations.fe_neg(h.X, h.X);

        FieldOperations.fe_mul(h.T, h.X, h.Y);
        return 0;
    }

    public static void ge_tobytes(byte[] s, int offset, GroupElementP2 h)
    {
        FieldElement recip = new FieldElement();
        FieldElement x = new FieldElement();
        FieldElement y = new FieldElement();

        FieldOperations.fe_invert(recip, h.Z);
        FieldOperations.fe_mul(x, h.X, recip);
        FieldOperations.fe_mul(y, h.Y, recip);
        FieldOperations.fe_tobytes(s, offset, y);
        s[offset + 31] ^= (byte) (FieldOperations.fe_isnegative(x) << 7);
    }

    private static void slide(byte[] r, byte[] a)
    {
        int i;
        int b;
        int k;

        for (i = 0; i < 256; ++i)
            r[i] = (byte) (1 & (a[i >> 3] >> (i & 7)));

        for (i = 0; i < 256; ++i)
            if (r[i] != 0)
            {
                for (b = 1; b <= 6 && i + b < 256; ++b)
                {
                    if (r[i + b] != 0)
                    {
                        if (r[i] + (r[i + b] << b) <= 15)
                        {
                            r[i] += (byte) (r[i + b] << b);
                            r[i + b] = 0;
                        }
                        else
                            if (r[i] - (r[i + b] << b) >= -15)
                            {
                                r[i] -= (byte) (r[i + b] << b);
                                for (k = i + b; k < 256; ++k)
                                {
                                    if (r[k] == 0)
                                    {
                                        r[k] = 1;
                                        break;
                                    }
                                    r[k] = 0;
                                }
                            }
                            else break;
                    }
                }
            }

    }

    /*
     * r = p
     */
    public static void ge_p1p1_to_p3(GroupElementP3 r, GroupElementP1P1 p)
    {
        FieldOperations.fe_mul(r.X, p.X, p.T);
        FieldOperations.fe_mul(r.Y, p.Y, p.Z);
        FieldOperations.fe_mul(r.Z, p.Z, p.T);
        FieldOperations.fe_mul(r.T, p.X, p.Y);
    }

    /*
     * r = p + q
     */

    public static void ge_add(GroupElementP1P1 r, GroupElementP3 p, GroupElementCached q)
    {
        FieldElement t0 = new FieldElement();

        /* qhasm: enter GroupElementadd */

        /* qhasm: fe X1 */

        /* qhasm: fe Y1 */

        /* qhasm: fe Z1 */

        /* qhasm: fe Z2 */

        /* qhasm: fe T1 */

        /* qhasm: fe ZZ */

        /* qhasm: fe YpX2 */

        /* qhasm: fe YmX2 */

        /* qhasm: fe T2d2 */

        /* qhasm: fe X3 */

        /* qhasm: fe Y3 */

        /* qhasm: fe Z3 */

        /* qhasm: fe T3 */

        /* qhasm: fe YpX1 */

        /* qhasm: fe YmX1 */

        /* qhasm: fe A */

        /* qhasm: fe B */

        /* qhasm: fe C */

        /* qhasm: fe D */

        /* qhasm: YpX1 = Y1+X1 */
        /* asm 1: fe_add(>YpX1=fe#1,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_add(>YpX1=r.X,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_add(r.X, p.Y, p.X);

        /* qhasm: YmX1 = Y1-X1 */
        /* asm 1: fe_sub(>YmX1=fe#2,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_sub(>YmX1=r.Y,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_sub(r.Y, p.Y, p.X);

        /* qhasm: A = YpX1*YpX2 */
        /* asm 1: fe_mul(>A=fe#3,<YpX1=fe#1,<YpX2=fe#15); */
        /* asm 2: fe_mul(>A=r.Z,<YpX1=r.X,<YpX2=q.YplusX); */
        FieldOperations.fe_mul(r.Z, r.X, q.YplusX);

        /* qhasm: B = YmX1*YmX2 */
        /* asm 1: fe_mul(>B=fe#2,<YmX1=fe#2,<YmX2=fe#16); */
        /* asm 2: fe_mul(>B=r.Y,<YmX1=r.Y,<YmX2=q.YminusX); */
        FieldOperations.fe_mul(r.Y, r.Y, q.YminusX);

        /* qhasm: C = T2d2*T1 */
        /* asm 1: fe_mul(>C=fe#4,<T2d2=fe#18,<T1=fe#14); */
        /* asm 2: fe_mul(>C=r.T,<T2d2=q.T2d,<T1=p.T); */
        FieldOperations.fe_mul(r.T, q.T2d, p.T);

        /* qhasm: ZZ = Z1*Z2 */
        /* asm 1: fe_mul(>ZZ=fe#1,<Z1=fe#13,<Z2=fe#17); */
        /* asm 2: fe_mul(>ZZ=r.X,<Z1=p.Z,<Z2=q.Z); */
        FieldOperations.fe_mul(r.X, p.Z, q.Z);

        /* qhasm: D = 2*ZZ */
        /* asm 1: fe_add(>D=fe#5,<ZZ=fe#1,<ZZ=fe#1); */
        /* asm 2: fe_add(>D=t0,<ZZ=r.X,<ZZ=r.X); */
        FieldOperations.fe_add(t0, r.X, r.X);

        /* qhasm: X3 = A-B */
        /* asm 1: fe_sub(>X3=fe#1,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_sub(>X3=r.X,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_sub(r.X, r.Z, r.Y);

        /* qhasm: Y3 = A+B */
        /* asm 1: fe_add(>Y3=fe#2,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_add(>Y3=r.Y,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_add(r.Y, r.Z, r.Y);

        /* qhasm: Z3 = D+C */
        /* asm 1: fe_add(>Z3=fe#3,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_add(>Z3=r.Z,<D=t0,<C=r.T); */
        FieldOperations.fe_add(r.Z, t0, r.T);

        /* qhasm: T3 = D-C */
        /* asm 1: fe_sub(>T3=fe#4,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_sub(>T3=r.T,<D=t0,<C=r.T); */
        FieldOperations.fe_sub(r.T, t0, r.T);

        /* qhasm: return */
    }

    public static void ge_p2_0(GroupElementP2 h)
    {
        FieldOperations.fe_0(h.X);
        FieldOperations.fe_1(h.Y);
        FieldOperations.fe_1(h.Z);
    }

    /*
     * r = p - q
     */

    public static void ge_sub(GroupElementP1P1 r, GroupElementP3 p, GroupElementCached q)
    {
        FieldElement t0 = new FieldElement();

        /* qhasm: enter ge_sub */

        /* qhasm: fe X1 */

        /* qhasm: fe Y1 */

        /* qhasm: fe Z1 */

        /* qhasm: fe Z2 */

        /* qhasm: fe T1 */

        /* qhasm: fe ZZ */

        /* qhasm: fe YpX2 */

        /* qhasm: fe YmX2 */

        /* qhasm: fe T2d2 */

        /* qhasm: fe X3 */

        /* qhasm: fe Y3 */

        /* qhasm: fe Z3 */

        /* qhasm: fe T3 */

        /* qhasm: fe YpX1 */

        /* qhasm: fe YmX1 */

        /* qhasm: fe A */

        /* qhasm: fe B */

        /* qhasm: fe C */

        /* qhasm: fe D */

        /* qhasm: YpX1 = Y1+X1 */
        /* asm 1: fe_add(>YpX1=fe#1,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_add(>YpX1=r.X,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_add(r.X, p.Y, p.X);

        /* qhasm: YmX1 = Y1-X1 */
        /* asm 1: fe_sub(>YmX1=fe#2,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_sub(>YmX1=r.Y,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_sub(r.Y, p.Y, p.X);

        /* qhasm: A = YpX1*YmX2 */
        /* asm 1: fe_mul(>A=fe#3,<YpX1=fe#1,<YmX2=fe#16); */
        /* asm 2: fe_mul(>A=r.Z,<YpX1=r.X,<YmX2=q.YminusX); */
        FieldOperations.fe_mul(r.Z, r.X, q.YminusX);

        /* qhasm: B = YmX1*YpX2 */
        /* asm 1: fe_mul(>B=fe#2,<YmX1=fe#2,<YpX2=fe#15); */
        /* asm 2: fe_mul(>B=r.Y,<YmX1=r.Y,<YpX2=q.YplusX); */
        FieldOperations.fe_mul(r.Y, r.Y, q.YplusX);

        /* qhasm: C = T2d2*T1 */
        /* asm 1: fe_mul(>C=fe#4,<T2d2=fe#18,<T1=fe#14); */
        /* asm 2: fe_mul(>C=r.T,<T2d2=q.T2d,<T1=p.T); */
        FieldOperations.fe_mul(r.T, q.T2d, p.T);

        /* qhasm: ZZ = Z1*Z2 */
        /* asm 1: fe_mul(>ZZ=fe#1,<Z1=fe#13,<Z2=fe#17); */
        /* asm 2: fe_mul(>ZZ=r.X,<Z1=p.Z,<Z2=q.Z); */
        FieldOperations.fe_mul(r.X, p.Z, q.Z);

        /* qhasm: D = 2*ZZ */
        /* asm 1: fe_add(>D=fe#5,<ZZ=fe#1,<ZZ=fe#1); */
        /* asm 2: fe_add(>D=t0,<ZZ=r.X,<ZZ=r.X); */
        FieldOperations.fe_add(t0, r.X, r.X);

        /* qhasm: X3 = A-B */
        /* asm 1: fe_sub(>X3=fe#1,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_sub(>X3=r.X,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_sub(r.X, r.Z, r.Y);

        /* qhasm: Y3 = A+B */
        /* asm 1: fe_add(>Y3=fe#2,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_add(>Y3=r.Y,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_add(r.Y, r.Z, r.Y);

        /* qhasm: Z3 = D-C */
        /* asm 1: fe_sub(>Z3=fe#3,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_sub(>Z3=r.Z,<D=t0,<C=r.T); */
        FieldOperations.fe_sub(r.Z, t0, r.T);

        /* qhasm: T3 = D+C */
        /* asm 1: fe_add(>T3=fe#4,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_add(>T3=r.T,<D=t0,<C=r.T); */
        FieldOperations.fe_add(r.T, t0, r.T);

        /* qhasm: return */
    }

    /*
     * r = p + q
     */
    public static void ge_madd(GroupElementP1P1 r, GroupElementP3 p, GroupElementPreComp q)
    {
        FieldElement t0 = new FieldElement();

        /* qhasm: enter ge_madd */

        /* qhasm: fe X1 */

        /* qhasm: fe Y1 */

        /* qhasm: fe Z1 */

        /* qhasm: fe T1 */

        /* qhasm: fe ypx2 */

        /* qhasm: fe ymx2 */

        /* qhasm: fe xy2d2 */

        /* qhasm: fe X3 */

        /* qhasm: fe Y3 */

        /* qhasm: fe Z3 */

        /* qhasm: fe T3 */

        /* qhasm: fe YpX1 */

        /* qhasm: fe YmX1 */

        /* qhasm: fe A */

        /* qhasm: fe B */

        /* qhasm: fe C */

        /* qhasm: fe D */

        /* qhasm: YpX1 = Y1+X1 */
        /* asm 1: fe_add(>YpX1=fe#1,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_add(>YpX1=r.X,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_add(r.X, p.Y, p.X);

        /* qhasm: YmX1 = Y1-X1 */
        /* asm 1: fe_sub(>YmX1=fe#2,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_sub(>YmX1=r.Y,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_sub(r.Y, p.Y, p.X);

        /* qhasm: A = YpX1*ypx2 */
        /* asm 1: fe_mul(>A=fe#3,<YpX1=fe#1,<ypx2=fe#15); */
        /* asm 2: fe_mul(>A=r.Z,<YpX1=r.X,<ypx2=q.yplusx); */
        FieldOperations.fe_mul(r.Z, r.X, q.yplusx);

        /* qhasm: B = YmX1*ymx2 */
        /* asm 1: fe_mul(>B=fe#2,<YmX1=fe#2,<ymx2=fe#16); */
        /* asm 2: fe_mul(>B=r.Y,<YmX1=r.Y,<ymx2=q.yminusx); */
        FieldOperations.fe_mul(r.Y, r.Y, q.yminusx);

        /* qhasm: C = xy2d2*T1 */
        /* asm 1: fe_mul(>C=fe#4,<xy2d2=fe#17,<T1=fe#14); */
        /* asm 2: fe_mul(>C=r.T,<xy2d2=q.xy2d,<T1=p.T); */
        FieldOperations.fe_mul(r.T, q.xy2d, p.T);

        /* qhasm: D = 2*Z1 */
        /* asm 1: fe_add(>D=fe#5,<Z1=fe#13,<Z1=fe#13); */
        /* asm 2: fe_add(>D=t0,<Z1=p.Z,<Z1=p.Z); */
        FieldOperations.fe_add(t0, p.Z, p.Z);

        /* qhasm: X3 = A-B */
        /* asm 1: fe_sub(>X3=fe#1,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_sub(>X3=r.X,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_sub(r.X, r.Z, r.Y);

        /* qhasm: Y3 = A+B */
        /* asm 1: fe_add(>Y3=fe#2,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_add(>Y3=r.Y,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_add(r.Y, r.Z, r.Y);

        /* qhasm: Z3 = D+C */
        /* asm 1: fe_add(>Z3=fe#3,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_add(>Z3=r.Z,<D=t0,<C=r.T); */
        FieldOperations.fe_add(r.Z, t0, r.T);

        /* qhasm: T3 = D-C */
        /* asm 1: fe_sub(>T3=fe#4,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_sub(>T3=r.T,<D=t0,<C=r.T); */
        FieldOperations.fe_sub(r.T, t0, r.T);

        /* qhasm: return */

    }

    /*
     * r = p - q
     */
    public static void ge_msub(GroupElementP1P1 r, GroupElementP3 p, GroupElementPreComp q)
    {
        FieldElement t0 = new FieldElement();

        /* qhasm: enter ge_msub */

        /* qhasm: fe X1 */

        /* qhasm: fe Y1 */

        /* qhasm: fe Z1 */

        /* qhasm: fe T1 */

        /* qhasm: fe ypx2 */

        /* qhasm: fe ymx2 */

        /* qhasm: fe xy2d2 */

        /* qhasm: fe X3 */

        /* qhasm: fe Y3 */

        /* qhasm: fe Z3 */

        /* qhasm: fe T3 */

        /* qhasm: fe YpX1 */

        /* qhasm: fe YmX1 */

        /* qhasm: fe A */

        /* qhasm: fe B */

        /* qhasm: fe C */

        /* qhasm: fe D */

        /* qhasm: YpX1 = Y1+X1 */
        /* asm 1: fe_add(>YpX1=fe#1,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_add(>YpX1=r.X,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_add(r.X, p.Y, p.X);

        /* qhasm: YmX1 = Y1-X1 */
        /* asm 1: fe_sub(>YmX1=fe#2,<Y1=fe#12,<X1=fe#11); */
        /* asm 2: fe_sub(>YmX1=r.Y,<Y1=p.Y,<X1=p.X); */
        FieldOperations.fe_sub(r.Y, p.Y, p.X);

        /* qhasm: A = YpX1*ymx2 */
        /* asm 1: fe_mul(>A=fe#3,<YpX1=fe#1,<ymx2=fe#16); */
        /* asm 2: fe_mul(>A=r.Z,<YpX1=r.X,<ymx2=q.yminusx); */
        FieldOperations.fe_mul(r.Z, r.X, q.yminusx);

        /* qhasm: B = YmX1*ypx2 */
        /* asm 1: fe_mul(>B=fe#2,<YmX1=fe#2,<ypx2=fe#15); */
        /* asm 2: fe_mul(>B=r.Y,<YmX1=r.Y,<ypx2=q.yplusx); */
        FieldOperations.fe_mul(r.Y, r.Y, q.yplusx);

        /* qhasm: C = xy2d2*T1 */
        /* asm 1: fe_mul(>C=fe#4,<xy2d2=fe#17,<T1=fe#14); */
        /* asm 2: fe_mul(>C=r.T,<xy2d2=q.xy2d,<T1=p.T); */
        FieldOperations.fe_mul(r.T, q.xy2d, p.T);

        /* qhasm: D = 2*Z1 */
        /* asm 1: fe_add(>D=fe#5,<Z1=fe#13,<Z1=fe#13); */
        /* asm 2: fe_add(>D=t0,<Z1=p.Z,<Z1=p.Z); */
        FieldOperations.fe_add(t0, p.Z, p.Z);

        /* qhasm: X3 = A-B */
        /* asm 1: fe_sub(>X3=fe#1,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_sub(>X3=r.X,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_sub(r.X, r.Z, r.Y);

        /* qhasm: Y3 = A+B */
        /* asm 1: fe_add(>Y3=fe#2,<A=fe#3,<B=fe#2); */
        /* asm 2: fe_add(>Y3=r.Y,<A=r.Z,<B=r.Y); */
        FieldOperations.fe_add(r.Y, r.Z, r.Y);

        /* qhasm: Z3 = D-C */
        /* asm 1: fe_sub(>Z3=fe#3,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_sub(>Z3=r.Z,<D=t0,<C=r.T); */
        FieldOperations.fe_sub(r.Z, t0, r.T);

        /* qhasm: T3 = D+C */
        /* asm 1: fe_add(>T3=fe#4,<D=fe#5,<C=fe#4); */
        /* asm 2: fe_add(>T3=r.T,<D=t0,<C=r.T); */
        FieldOperations.fe_add(r.T, t0, r.T);

        /* qhasm: return */

    }

    /*
     * r = p
     */
    public static void ge_p1p1_to_p2(GroupElementP2 r, GroupElementP1P1 p)
    {
        FieldOperations.fe_mul(r.X, p.X, p.T);
        FieldOperations.fe_mul(r.Y, p.Y, p.Z);
        FieldOperations.fe_mul(r.Z, p.Z, p.T);
    }

    /*
     * r = a * A + b * B where a = a[0]+256*a[1]+...+256^31 a[31]. and b =
     * b[0]+256*b[1]+...+256^31 b[31]. B is the Ed25519 base point (x,4/5) with
     * x positive.
     */

    public static void ge_double_scalarmult_vartime(GroupElementP2 r, byte[] a, GroupElementP3 A, byte[] b)
    {
        GroupElementPreComp[] Bi = LookupTables.Base2;
        // todo: Perhaps remove these allocations?
        byte[] aslide = new byte[256];
        byte[] bslide = new byte[256];
        GroupElementCached[] Ai = new GroupElementCached[8]; /*
                                                              * A,3A,5A,7A,9A,11A
                                                              * ,13A,15A
                                                              */
        Ai[0] = new GroupElementCached();
        Ai[1] = new GroupElementCached();
        Ai[2] = new GroupElementCached();
        Ai[3] = new GroupElementCached();
        Ai[4] = new GroupElementCached();
        Ai[5] = new GroupElementCached();
        Ai[6] = new GroupElementCached();
        Ai[7] = new GroupElementCached();

        GroupElementP1P1 t = new GroupElementP1P1();
        GroupElementP3 u = new GroupElementP3();
        GroupElementP3 A2 = new GroupElementP3();
        int i;

        slide(aslide, a);
        slide(bslide, b);

        ge_p3_to_cached(Ai[0], A);
        ge_p3_dbl(t, A);
        ge_p1p1_to_p3(A2, t);
        ge_add(t, A2, Ai[0]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[1], u);
        ge_add(t, A2, Ai[1]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[2], u);
        ge_add(t, A2, Ai[2]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[3], u);
        ge_add(t, A2, Ai[3]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[4], u);
        ge_add(t, A2, Ai[4]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[5], u);
        ge_add(t, A2, Ai[5]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[6], u);
        ge_add(t, A2, Ai[6]);
        ge_p1p1_to_p3(u, t);
        ge_p3_to_cached(Ai[7], u);

        ge_p2_0(r);

        for (i = 255; i >= 0; --i)
        {
            if ((aslide[i] != 0) || (bslide[i] != 0)) break;
        }

        for (; i >= 0; --i)
        {
            ge_p2_dbl(t, r);

            if (aslide[i] > 0)
            {
                ge_p1p1_to_p3(u, t);
                ge_add(t, u, Ai[aslide[i] / 2]);
            }
            else
                if (aslide[i] < 0)
                {
                    ge_p1p1_to_p3(u, t);
                    ge_sub(t, u, Ai[(-aslide[i]) / 2]);
                }

            if (bslide[i] > 0)
            {
                ge_p1p1_to_p3(u, t);
                ge_madd(t, u, Bi[bslide[i] / 2]);
            }
            else
                if (bslide[i] < 0)
                {
                    ge_p1p1_to_p3(u, t);
                    ge_msub(t, u, Bi[(-bslide[i]) / 2]);
                }

            ge_p1p1_to_p2(r, t);
        }
    }

    /*
     * r = p
     */
    public static void ge_p3_to_cached(GroupElementCached r, GroupElementP3 p)
    {
        FieldOperations.fe_add(r.YplusX, p.Y, p.X);
        FieldOperations.fe_sub(r.YminusX, p.Y, p.X);
        FieldOperations.fe_cmov(r.Z, p.Z, 1);
        FieldOperations.fe_mul(r.T2d, p.T, LookupTables.d2);
    }

    public static void ge_p3_to_p2(GroupElementP2 r, GroupElementP3 p)
    {
        r.X = p.X;
        r.Y = p.Y;
        r.Z = p.Z;
    }

    /*
     * r = 2 * p
     */

    public static void ge_p2_dbl(GroupElementP1P1 r, GroupElementP2 p)
    {
        FieldElement t0 = new FieldElement();

        /* qhasm: enter ge_p2_dbl */

        /* qhasm: fe X1 */

        /* qhasm: fe Y1 */

        /* qhasm: fe Z1 */

        /* qhasm: fe A */

        /* qhasm: fe AA */

        /* qhasm: fe XX */

        /* qhasm: fe YY */

        /* qhasm: fe B */

        /* qhasm: fe X3 */

        /* qhasm: fe Y3 */

        /* qhasm: fe Z3 */

        /* qhasm: fe T3 */

        /* qhasm: XX=X1^2 */
        /* asm 1: fe_sq(>XX=fe#1,<X1=fe#11); */
        /* asm 2: fe_sq(>XX=r.X,<X1=p.X); */
        FieldOperations.fe_sq(r.X, p.X);

        /* qhasm: YY=Y1^2 */
        /* asm 1: fe_sq(>YY=fe#3,<Y1=fe#12); */
        /* asm 2: fe_sq(>YY=r.Z,<Y1=p.Y); */
        FieldOperations.fe_sq(r.Z, p.Y);

        /* qhasm: B=2*Z1^2 */
        /* asm 1: fe_sq2(>B=fe#4,<Z1=fe#13); */
        /* asm 2: fe_sq2(>B=r.T,<Z1=p.Z); */
        FieldOperations.fe_sq2(r.T, p.Z);

        /* qhasm: A=X1+Y1 */
        /* asm 1: fe_add(>A=fe#2,<X1=fe#11,<Y1=fe#12); */
        /* asm 2: fe_add(>A=r.Y,<X1=p.X,<Y1=p.Y); */
        FieldOperations.fe_add(r.Y, p.X, p.Y);

        /* qhasm: AA=A^2 */
        /* asm 1: fe_sq(>AA=fe#5,<A=fe#2); */
        /* asm 2: fe_sq(>AA=t0,<A=r.Y); */
        FieldOperations.fe_sq(t0, r.Y);

        /* qhasm: Y3=YY+XX */
        /* asm 1: fe_add(>Y3=fe#2,<YY=fe#3,<XX=fe#1); */
        /* asm 2: fe_add(>Y3=r.Y,<YY=r.Z,<XX=r.X); */
        FieldOperations.fe_add(r.Y, r.Z, r.X);

        /* qhasm: Z3=YY-XX */
        /* asm 1: fe_sub(>Z3=fe#3,<YY=fe#3,<XX=fe#1); */
        /* asm 2: fe_sub(>Z3=r.Z,<YY=r.Z,<XX=r.X); */
        FieldOperations.fe_sub(r.Z, r.Z, r.X);

        /* qhasm: X3=AA-Y3 */
        /* asm 1: fe_sub(>X3=fe#1,<AA=fe#5,<Y3=fe#2); */
        /* asm 2: fe_sub(>X3=r.X,<AA=t0,<Y3=r.Y); */
        FieldOperations.fe_sub(r.X, t0, r.Y);

        /* qhasm: T3=B-Z3 */
        /* asm 1: fe_sub(>T3=fe#4,<B=fe#4,<Z3=fe#3); */
        /* asm 2: fe_sub(>T3=r.T,<B=r.T,<Z3=r.Z); */
        FieldOperations.fe_sub(r.T, r.T, r.Z);

        /* qhasm: return */

    }

    public static void ge_p3_dbl(GroupElementP1P1 r, GroupElementP3 p)
    {
        GroupElementP2 q = new GroupElementP2();
        ge_p3_to_p2(q, p);
        ge_p2_dbl(r, q);
    }

    static byte equal(byte b, int i)
    {

        // byte ub = b;
        // byte uc = (byte) i;
        // byte x = (byte) (ub ^ uc); /* 0: yes; 1..255: no */
        // int y = x; /* 0: yes; 1..255: no */
        // {
        // y -= 1;
        // } /* 4294967295: yes; 0..254: no */
        // y >>= 31; /* 1: yes; 0: no */
        //
        return (byte) ((b == i) ? 1 : 0);

        // return (byte) y;
    }

    static byte negative(byte b)
    {
        // long x = ((long) (long) b);
        // x >>= 63; /* 1: yes; 0: no
        // return (byte) x;

        return (byte) ((b < 0) ? 1 : 0);
    }

    static void cmov(GroupElementPreComp t, GroupElementPreComp u, byte b)
    {
        FieldOperations.fe_cmov(t.yplusx, u.yplusx, b);
        FieldOperations.fe_cmov(t.yminusx, u.yminusx, b);
        FieldOperations.fe_cmov(t.xy2d, u.xy2d, b);
    }

    public static void ge_precomp_0(GroupElementPreComp h)
    {
        FieldOperations.fe_1(h.yplusx);
        FieldOperations.fe_1(h.yminusx);
        FieldOperations.fe_0(h.xy2d);
    }

    static void select(GroupElementPreComp t, int pos, byte b)
    {
        GroupElementPreComp minust = new GroupElementPreComp();
        // byte bnegative = negative(b);
        // byte babs = (byte) (b - (((-bnegative) & b) << 1));

        byte bnegative = (byte) ((b < 0) ? 1 : 0);
        byte babs = (byte) Math.abs(b);

        ge_precomp_0(t);
        GroupElementPreComp[] table = LookupTables.Base[pos];
        cmov(t, table[0], equal(babs, 1));
        cmov(t, table[1], equal(babs, 2));
        cmov(t, table[2], equal(babs, 3));
        cmov(t, table[3], equal(babs, 4));
        cmov(t, table[4], equal(babs, 5));
        cmov(t, table[5], equal(babs, 6));
        cmov(t, table[6], equal(babs, 7));
        cmov(t, table[7], equal(babs, 8));

        FieldOperations.fe_cmov(minust.yplusx, t.yminusx, 1);
        FieldOperations.fe_cmov(minust.yminusx, t.yplusx, 1);
        // minust.yplusx = t.yminusx;
        // minust.yminusx = t.yplusx;

        FieldOperations.fe_neg(minust.xy2d, t.xy2d);
        cmov(t, minust, bnegative);
    }

    public static void ge_p3_0(GroupElementP3 h)
    {
        FieldOperations.fe_0(h.X);
        FieldOperations.fe_1(h.Y);
        FieldOperations.fe_1(h.Z);
        FieldOperations.fe_0(h.T);
    }

    /*
     * h = a * B where a = a[0]+256*a[1]+...+256^31 a[31] B is the Ed25519 base
     * point (x,4/5) with x positive.
     * 
     * Preconditions: a[31] <= 127
     */

    public static void ge_scalarmult_base(GroupElementP3 h, byte[] a, int offset)
    {
        // todo: Perhaps remove this allocation
        byte[] e = new byte[64];
        byte carry;
        GroupElementP1P1 r = new GroupElementP1P1();
        GroupElementP2 s = new GroupElementP2();
        GroupElementPreComp t = new GroupElementPreComp();
        int i;

        for (i = 0; i < 32; ++i)
        {
            e[2 * i + 0] = (byte) ((a[offset + i] >> 0) & 15);
            e[2 * i + 1] = (byte) ((a[offset + i] >> 4) & 15);
        }
        /* each e[i] is between 0 and 15 */
        /* e[63] is between 0 and 7 */

        carry = 0;
        for (i = 0; i < 63; ++i)
        {
            e[i] += carry;
            carry = (byte) (e[i] + 8);
            carry >>= 4;
            e[i] -= (byte) (carry << 4);
        }
        e[63] += carry;
        /* each e[i] is between -8 and 8 */

        ge_p3_0(h);
        for (i = 1; i < 64; i += 2)
        {
            select(t, i / 2, e[i]);
            ge_madd(r, h, t);
            ge_p1p1_to_p3(h, r);
        }

        ge_p3_dbl(r, h);
        ge_p1p1_to_p2(s, r);
        ge_p2_dbl(r, s);
        ge_p1p1_to_p2(s, r);
        ge_p2_dbl(r, s);
        ge_p1p1_to_p2(s, r);
        ge_p2_dbl(r, s);
        ge_p1p1_to_p3(h, r);

        for (i = 0; i < 64; i += 2)
        {
            select(t, i / 2, e[i]);
            ge_madd(r, h, t);
            ge_p1p1_to_p3(h, r);
        }
    }

    // TODO
    public static void ge_scalarmult_vartime(GroupElementP1P1 t, byte[] a, GroupElementP3 A) {
        GroupElementPreComp[] base2 = LookupTables.Base2;

        byte[] r1 = new byte[256];

        GroupElementCached[] span = new GroupElementCached[8];
        for (int i = 0; i < 8; i ++) span[i] = new GroupElementCached();

        GroupOperations.slide(r1, a);

        GroupOperations.ge_p3_to_cached(span[0], A);

        GroupOperations.ge_p3_dbl(t, A);

        GroupElementP3 r2 = new GroupElementP3();
        GroupOperations.ge_p1p1_to_p3(r2, t);

        GroupOperations.ge_add(t, r2, span[0]);

        GroupElementP3 r3 = new GroupElementP3();
        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[1], r3);

        GroupOperations.ge_add(t, r2, span[1]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[2], r3);

        GroupOperations.ge_add(t, r2, span[2]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[3], r3);

        GroupOperations.ge_add(t, r2, span[3]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[4], r3);

        GroupOperations.ge_add(t, r2, span[4]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[5], r3);

        GroupOperations.ge_add(t, r2, span[5]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[6], r3);

        GroupOperations.ge_add(t, r2, span[6]);

        GroupOperations.ge_p1p1_to_p3(r3, t);

        GroupOperations.ge_p3_to_cached(span[7], r3);

        GroupElementP2 p = new GroupElementP2();
        GroupOperations.ge_p2_0(p);

        int maxValue = 255;

        while (maxValue >= 0 && (int) r1[maxValue] == 0)
            -- maxValue;

        for (; maxValue >= 0; --maxValue) {
            GroupOperations.ge_p2_dbl(t, p);

            if (r1[maxValue] > 0) {
                GroupOperations.ge_p1p1_to_p3(r3, t);

                GroupOperations.ge_add(t, r3, span[r1[maxValue] / 2]);
            } else {
                if (r1[maxValue] < 0) {
                    GroupOperations.ge_p1p1_to_p3(r3, t);

                    GroupOperations.ge_sub(t, r3, span[-(r1[maxValue]) / 2]);
                }
            }

            GroupOperations.ge_p1p1_to_p2(p, t);
        }
    }

    // TODO
    public static void ge_scalarmult_vartime(GroupElementP2 r, byte[] a, GroupElementP3 A) {
        GroupElementPreComp[] base2 = LookupTables.Base2;

        byte[] r1 = new byte[256];

        GroupElementCached[] span = new GroupElementCached[8];
        for (int i = 0; i < 8; i ++) span[i] = new GroupElementCached();

        GroupOperations.slide(r1, a);

        GroupOperations.ge_p3_to_cached(span[0], A);

        GroupElementP1P1 r2 = new GroupElementP1P1();
        GroupOperations.ge_p3_dbl(r2, A);

        GroupElementP3 r3 = new GroupElementP3();
        GroupOperations.ge_p1p1_to_p3(r3, r2);

        GroupOperations.ge_add(r2, r3, span[0]);

        GroupElementP3 r4 = new GroupElementP3();
        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[1], r4);

        GroupOperations.ge_add(r2, r3, span[1]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[2], r4);

        GroupOperations.ge_add(r2, r3, span[2]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[3], r4);

        GroupOperations.ge_add(r2, r3, span[3]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[4], r4);

        GroupOperations.ge_add(r2, r3, span[4]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[5], r4);

        GroupOperations.ge_add(r2, r3, span[5]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[6], r4);

        GroupOperations.ge_add(r2, r3, span[6]);

        GroupOperations.ge_p1p1_to_p3(r4, r2);

        GroupOperations.ge_p3_to_cached(span[7], r4);

        GroupOperations.ge_p2_0(r);

        int maxValue = 255;

        while (maxValue >= 0 && (int) r1[maxValue] == 0)
            -- maxValue;

        for (; maxValue >= 0; --maxValue) {
            GroupOperations.ge_p2_dbl(r2, r);

            if (r1[maxValue] > 0) {
                GroupOperations.ge_p1p1_to_p3(r4, r2);

                GroupOperations.ge_add(r2, r4, span[r1[maxValue] / 2]);
            } else {
                if (r1[maxValue] < 0) {
                    GroupOperations.ge_p1p1_to_p3(r4, r2);

                    GroupOperations.ge_sub(r2, r4, span[-(r1[maxValue]) / 2]);
                }
            }

            GroupOperations.ge_p1p1_to_p2(r, r2);
        }
    }

    public static void ge_p3_tobytes(byte[] s, int offset, GroupElementP3 h)
    {
        FieldElement recip = new FieldElement();
        FieldElement h1 = new FieldElement();
        FieldElement h2 = new FieldElement();

        FieldOperations.fe_invert(recip, h.Z);
        FieldOperations.fe_mul(h1, h.X, recip);
        FieldOperations.fe_mul(h2, h.Y, recip);
        FieldOperations.fe_tobytes(s, offset, h2);

        s[offset + 31] ^= (byte) (FieldOperations.fe_isnegative(h1) << 7);
    }
}
