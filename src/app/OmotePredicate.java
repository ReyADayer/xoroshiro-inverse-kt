package app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

class OmotePredicate implements IntPredicate {

    final int flawlessIdx;
    final int[] ivs = new int[6];
    final int[] flaws = new int[5];
    final int ability;
    final int nature;
    final int[] nextlower = new int[6];
    final int[] nextupper = new int[6];
    final int nextability;
    final int nextnature;
    private int flawlessIvs = 1;

    final Matrix finv = Matrix.finv();

    final long xoroshiroequiv;
    {
        int[] x = DenGenerator.linear(0);
        long tmp = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0) {
                tmp ^= finv.rows[i];
            }
        }
        xoroshiroequiv = tmp;
    }

    final Matrix f = Matrix.f();
    final long[] kernelbasis;
    {
        List<Long> basislist = f.kernel();
        int l = basislist.size();
        kernelbasis = new long[l];
        for (int i = 0; i < l; i++) {
            kernelbasis[i] = basislist.get(i);
        }
    }
    final long[] kernel;
    {
        int dim = kernelbasis.length;
        int card = 1 << dim;
        List<Long> tmp = new ArrayList<>(card);
        for (int i = 0; i < card; i++) {
            long k = 0;
            for (int j = 0; j < dim; j++) {
                if ((i & (1 << j)) != 0) {
                    k ^= kernelbasis[j];
                }
            }
            tmp.add(k);
        }
        this.kernel = new long[tmp.size()];
        for (int i = 0; i < tmp.size(); i++) {
            this.kernel[i] = tmp.get(i);
        }
    }

    /*
     * find x (64 bit)
     * f(seed,Xoroshiroconst) = {flawlessIdx0, flawlessIdx1, ... , ability}
     * f(seed) = f(Xoroshiroconst) + {flawlessIdx0, flawlessIdx1, ... , ability}
     * f:surj
     * seed = f^-1(f(Xoroshiroconst)) + f^-1({flawlessIdx0, flawlessIdx1, ... , ability})
     *      + kernel
     */

    public OmotePredicate(int flawlessIdx, int[] ivs, int ability, int nature, int[] nextlower, int[] nextupper,
            int nextability, int nextnature) {
        this.flawlessIdx = flawlessIdx;
        List<Integer> flaws = new ArrayList<>();
        for (int i = 0; i < ivs.length; i++) {
            this.ivs[i] = ivs[i];
            if (ivs[i] != 31) {
                flaws.add(ivs[i]);
            }
        }
        for (int i = 0; i < this.flaws.length; i++) {
            this.flaws[i] = flaws.get(i);
        }
        this.ability = ability;
        this.nature = nature;
        for (int i = 0; i < this.nextlower.length; i++) {
            this.nextlower[i] = nextlower[i];
        }
        for (int i = 0; i < this.nextupper.length; i++) {
            this.nextupper[i] = nextupper[i];
        }
        this.nextability = nextability;
        this.nextnature = nextnature;
    }

    @Override
    public boolean test(int omote) {
        int[] iv0 = new int[5];
        int[] iv1 = new int[5];
        for (int i = iv0.length - 1; i >= 0; i--) {
            int iv = omote & 0x1f;
            iv0[i] = iv;
            iv1[i] = (flaws[i] - iv) & 0x1f;
            omote >>>= 5;
        }
        int flawlessIdx0 = omote & 0x7;
        int flawlessIdx1 = (flawlessIdx - flawlessIdx0) & 0x7;

        int[] x = new int[57];
        writeBE(flawlessIdx0, x, 0, 3);
        writeBE(flawlessIdx1, x, 3, 3);
        for (int i = 0; i < 5; i++) {
            writeBE(iv0[i], x, 6 + 10 * i, 5);
            writeBE(iv1[i], x, 11 + 10 * i, 5);
        }
        writeBE(ability, x, 56, 1);
        long finvx = 0;
        for (int i = 0; i < x.length; i++) {
            if (x[i] != 0) {
                finvx ^= finv.rows[i];
            }
        }
        for (int i = 0; i < kernel.length; i++) {
            long seed0 = xoroshiroequiv ^ finvx ^ kernel[i];
            long seed1 = seed0 + XOROSHIRO_CONST;
            if (ivAbilityNature(seed1, flawlessIvs, nextlower, nextupper, nextability, nextnature)) {
                if (ivAbilityNature(seed0, flawlessIvs, ivs, ivs, ability, nature)) {
                    System.out.printf("0x%016x%n", seed0);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean ivAbilityNature(long seed, int flawlessIvs, int[] upper, int[] lower, int ability,
            int nature) {
        long s0 = seed;
        long s1 = XOROSHIRO_CONST;
        for (int i = 0; i < 3; i++) {
            s1 ^= s0;
            s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
            s1 = rotl(s1, 37);
        }
        int[] tmpivs = { -1, -1, -1, -1, -1, -1 };
        for (int i = 0; i < flawlessIvs; i++) {
            int idx;
            do {
                do {
                    int temper = (int) (s0 + s1);
                    idx = temper & 0x7;
                    s1 ^= s0;
                    s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                    s1 = rotl(s1, 37);
                } while (idx >= 6);
            } while (tmpivs[idx] != -1);
            tmpivs[idx] = 31;
            if (upper[idx] < 31) {
                return false;
            }
        }
        for (int i = 0; i < 6; i++) {
            if (tmpivs[i] == -1) {
                int temper = (int) (s0 + s1);
                int iv = temper & 0x1f;
                tmpivs[i] = iv;
                if (iv < lower[i] || iv > upper[i]) {
                    return false;
                }
                s1 ^= s0;
                s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                s1 = rotl(s1, 37);
            }
        }
        int tmpability;
        {
            int temper = (int) (s0 + s1);
            s1 ^= s0;
            s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
            s1 = rotl(s1, 37);
            tmpability = temper & 0x1;
            if (ability != tmpability) {
                return false;
            }
        }
        { // gender
            s1 ^= s0;
            s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
            s1 = rotl(s1, 37);
        }
        int tmpnature;
        {
            do {
                int temper = (int) (s0 + s1);
                tmpnature = temper & 0x1f;
                s1 ^= s0;
                s0 = rotl(s0, 24) ^ s1 ^ (s1 << 16);
                s1 = rotl(s1, 37);
            } while (tmpnature >= 25);
            if (nature != tmpnature) {
                return false;
            }
        }
        return true;
    }

    private static void writeBE(int src, int[] dst, int start, int length) {
        for (int i = 0; i < length; i++) {
            int b = 1 << (length - 1 - i);
            if ((src & b) != 0) {
                dst[start + i] = 1;
            } else {
                dst[start + i] = 0;
            }
        }
    }

    public static final long XOROSHIRO_CONST = 0x82A2B175229D6A5BL;

    static long rotl(long x, int k) {
        return (x << k) | (x >>> (64 - k));
    }

    static class IVPredicateBuilder {
        int flawlessIdx;
        int[] ivs = new int[5];
        int ability;
        int nature;
        int[] nextupper = new int[6];
        int[] nextlower = new int[6];
        int nextability;
        int nextnature;

        public OmotePredicate getIncetance() {
            return new OmotePredicate(flawlessIdx, ivs, ability, nature, nextlower, nextupper, nextability, nextnature);
        }

        public IVPredicateBuilder setFlawlessIdx(int flawlessIdx) {
            this.flawlessIdx = flawlessIdx;
            return this;
        }

        public IVPredicateBuilder setIvs(int[] ivs) {
            this.ivs = ivs;
            return this;
        }

        public IVPredicateBuilder setAbility(int ability) {
            this.ability = ability;
            return this;
        }

        public IVPredicateBuilder setNextLower(int[] ivs) {
            this.nextlower = ivs;
            return this;
        }

        public IVPredicateBuilder setNextUpper(int[] ivs) {
            this.nextupper = ivs;
            return this;
        }

        public IVPredicateBuilder setNextAbility(int nextability) {
            this.nextability = nextability;
            return this;
        }

        public IVPredicateBuilder setNature(int nature) {
            this.nature = nature;
            return this;
        }

        public IVPredicateBuilder setNextnature(int nextnature) {
            this.nextnature = nextnature;
            return this;
        }

    }
}