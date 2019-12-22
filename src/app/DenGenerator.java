package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DenGenerator {
    public static int flawlessIvs = 1;

    public static List<Integer> original(long seed/* , Entry pkmn */) {
        Xoroshiro rng = new Xoroshiro(seed);
        int EC = (int) rng.nextInt();
        int SIDTID = (int) rng.nextInt();
        int PID = (int) rng.nextInt();
        int shinytype = GetShinyType(PID, SIDTID);
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
        int gender = (int)rng.nextInt(253) + 1;
        int nature = (int)rng.nextInt(25);
        
        System.out.printf("seed:0x%016x%n", seed);
        // System.out.printf("EC:%08x%n", EC);
        // System.out.printf("SIDTID:%08x%n", SIDTID);
        // System.out.printf("PID:%08x%n", PID);
        // System.out.printf("shinytype:%d%n", shinytype);
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

    public static int[] pure(long s0, long s1) {
        int m = 57;
        int[] column = new int[m];
        Xoroshiro rng = new Xoroshiro(s0, s1);
        rng.next();
        rng.next();
        rng.next();
        writeBE(rng.s[0], column, 0, 3);
        writeBE(rng.s[1], column, 3, 3);
        rng.next();
        for (int i = 0; i < 5; i++) {
            writeBE(rng.s[0], column, 6 + 10 * i, 5);
            writeBE(rng.s[1], column, 11 + 10 * i, 5);
            rng.next();
        }
        writeBE(rng.next(), column, 56, 1);
        return column;
    }

    public static int[] pure(long seed){
        return pure(seed, Xoroshiro.XOROSHIRO_CONST);
    }

    public static int[] linear(long seed) {
        Matrix f = Matrix.f();
        int[] v = f.multVect(seed);
        int[] o = pure(0, Xoroshiro.XOROSHIRO_CONST);
        for (int i = 0; i < o.length; i++) {
            v[i] ^= o[i];
        }
        return v;
    }

    static boolean isGood(long seed) {
        /* least iv rollment and no flawless ivs in chance */
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
        if ((int) (end - start) != flawlessIvs) {
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

    private static void writeBE(long src, int[] dst, int start, int length) {
        for (int i = 0; i < length; i++) {
            long b = 1L << (length - 1 - i);
            if ((src & b) != 0) {
                dst[start + i] = 1;
            } else {
                dst[start + i] = 0;
            }
        }
    }

    private static int GetShinyType(int pid, int tidsid) {
        int a = (pid >>> 16) ^ (tidsid >>> 16);
        int b = (pid & 0xFFFF) ^ (tidsid & 0xFFFF);
        if (a == b) {
            return 2; // square
        } else if ((a ^ b) < 0x10) {
            return 1; // star
        }
        return 0;
    }
}