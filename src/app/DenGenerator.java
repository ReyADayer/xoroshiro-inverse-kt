package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DenGenerator {
    public static List<Integer> original(long seed, int flawlessIvs) {
        Xoroshiro rng = new Xoroshiro(seed);
        for (int i = 0; i < 3; i++) {
            rng.next();
        }
        int[] ivs = { -1, -1, -1, -1, -1, -1 };
        long start = rng.i;
        for (int i = 0; i < flawlessIvs; i++) {
            int idx;
            do {
                idx = (int) rng.nextInt(6);
            } while (ivs[idx] != -1);
            ivs[idx] = 31;
        }
        for (int i = 0; i < 6; i++) {
            if (ivs[i] == -1)
                ivs[i] = (int) rng.nextInt(32);
        }
        long end = rng.i;
        int ability = (int) rng.nextInt(2);
        int gender = (int) rng.nextInt(253) + 1;
        int nature = (int) rng.nextInt(25);

        System.out.printf("seed:0x%016x%n", seed);
        System.out.printf("IVs:%s%n", Arrays.toString(ivs));
        System.out.printf("IV rollment:%d%n", end - start);
        System.out.printf("ability:%d%n", ability);
        System.out.printf("gender:%d%n", gender);
        System.out.printf("nature:%d%n", nature);
        List<Integer> result = new ArrayList<>(7);
        for (int iv : ivs) {
            result.add(iv);
        }
        result.add(ability);
        result.add(gender);
        result.add(nature);
        return result;
    }

    public static byte[] pure(long s0, long s1, int flawlessIvs, int ivRerollment) {
        int m = 57;
        byte[] column = new byte[m];
        Xoroshiro rng = new Xoroshiro(s0, s1);
        rng.next();
        rng.next();
        rng.next();
        for (int i = 0; i < ivRerollment; i++) {
            rng.next();
        }
        writeBE(rng.s[0], column, 0, 3);
        writeBE(rng.s[1], column, 3, 3);
        rng.next();
        for (int i = 0; i < 6 - flawlessIvs; i++) {
            writeBE(rng.s[0], column, 6 + 10 * i, 5);
            writeBE(rng.s[1], column, 11 + 10 * i, 5);
            rng.next();
        }
        writeBE(rng.next(), column, 56, 1);
        return column;
    }

    public static byte[] pure(long seed, int flawlessIvs, int ivRerollment) {
        return pure(seed, Xoroshiro.XOROSHIRO_CONST, flawlessIvs, ivRerollment);
    }

    public static byte[] linear(long seed, int flawlessIvs, int ivRerollment) {
        Matrix f = Matrix.f(flawlessIvs, ivRerollment);
        byte[] v = f.multVect(seed);
        byte[] o = pure(0, Xoroshiro.XOROSHIRO_CONST, flawlessIvs, ivRerollment);
        for (int i = 0; i < o.length; i++) {
            v[i] ^= o[i];
        }
        return v;
    }

    static boolean isGood(long seed, int flawlessIvs, int ivRerollment) {
        /* specified iv rollment and no 31 ivs in chance */
        Xoroshiro rng = new Xoroshiro(seed);
        rng.nextInt();
        rng.nextInt();
        rng.nextInt();
        int[] ivs = { -1, -1, -1, -1, -1, -1 };
        long start = rng.i;
        for (int i = 0; i < flawlessIvs; i++) {
            int idx;
            do {
                idx = (int) rng.nextInt(6);
            } while (ivs[idx] != -1);
            ivs[idx] = 31;
        }
        long end = rng.i;
        if ((int) (end - start) != flawlessIvs + ivRerollment) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (ivs[i] == -1) {
                int iv = (int) rng.nextInt(32);
                if (iv == 31)
                    return false;
                ivs[i] = iv;
            }
        }
        return true;
    }

    private static void writeBE(long src, byte[] dst, int start, int length) {
        for (int i = 0; i < length; i++) {
            int b = 1 << (length - 1 - i);
            if ((src & b) != 0) {
                dst[start + i] = 1;
            } else {
                dst[start + i] = 0;
            }
        }
    }
}