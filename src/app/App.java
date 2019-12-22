package app;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.IntStream;

public class App {
    public final static String VERSION = "1.0";
    public final static String MESSAGE_01 = "enter pricise IVs of the 1st raid pokemon";
    public final static String MESSAGE_02 = "enter index of ability of the 1st raid pokemon";
    public final static String MESSAGE_l2 = "enter lower bounds of IVs of 2nd raid pokemon";
    public final static String MESSAGE_u2 = "enter upper bounds of IVs of 2nd raid pokemon";
    public final static String MESSAGE_i2 = "enter index of ability of the 2nd raid pokemon";
    public final static String MESSAGE_n1 = "enter nature of 1st raid pokemon";
    public final static String MESSAGE_n2 = "enter nature of 2nd raid pokemon";

    public final static Map<String, Integer> naturedict = new HashMap<>();
    static {
        // String[] ja = { "がんばりや", "さみしがり", "ゆうかん", "いじっぱり", "やんちゃ", "ずぶとい", "すなお", "のんき", "わんぱく", "のうてんき", "おくびょう",
        //         "せっかち", "まじめ", "ようき", "むじゃき", "ひかえめ", "おっとり", "れいせい", "てれや", "うっかりや", "おだやか", "おとなしい", "なまいき", "しんちょう",
        //         "きまぐれ" };
        String[] en_capital = { "Hardy", "Lonely", "Brave", "Adamant", "Naughty", "Bold", "Docile", "Relaxed", "Impish",
                "Lax", "Timid", "Hasty", "Serious", "Jolly", "Naive", "Modest", "Mild", "Quiet", "Bashful", "Rash",
                "Calm", "Gentle", "Sassy", "Careful", "Quirky" };
        String[] en_small = { "hardy", "lonely", "brave", "adamant", "naughty", "bold", "docile", "relaxed", "impish",
                "lax", "timid", "hasty", "serious", "jolly", "naive", "modest", "mild", "quiet", "bashful", "rash",
                "calm", "gentle", "sassy", "careful", "quirky" };
        for (int i = 0; i < en_small.length; i++) {
            // naturedict.put(ja[i], i);
            naturedict.put(en_capital[i], i);
            naturedict.put(en_small[i], i);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("xoroshiro inverse %s%n", VERSION);
        System.out.println("press ctrl+c to exit");
        boolean debug = false;
        if ((args != null) && args.length == 1) {
            debug = "--debug".equals(args[0]);
        }
        loop(debug);
    }

    private static void loop(boolean debug) {
        int[] ivs1 = new int[6];
        int[] lower = new int[6];
        int[] upper = new int[6];
        int ability1, ability2;
        int omote_lower_bound_inclusive, omote_upper_bound_inclusive;
        int nature1, nature2;
        if (debug) {
            SecureRandom r = null;
            try {
                r = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if (r == null) {
                return;
            }
            long goodseed, nextseed;
            while (true) {
                goodseed = r.nextLong();
                if (DenGenerator.isGood(goodseed))
                    break;
            }
            nextseed = goodseed + Xoroshiro.XOROSHIRO_CONST;
            List<Integer> g = DenGenerator.original(goodseed);
            List<Integer> h = DenGenerator.original(nextseed);
            for (int i = 0; i < 6; i++) {
                ivs1[i] = g.get(i);
                lower[i] = h.get(i);
                upper[i] = h.get(i);
            }
            ability1 = g.get(6);
            ability2 = h.get(6);
            nature1 = g.get(8);
            nature2 = h.get(8);
            int[] x = DenGenerator.pure(goodseed);
            int omote = (int) readBE(x, 0, 3);
            for (int i = 0; i < 5; i++) {
                omote = (omote << 5) | (int) readBE(x, 6 + 10 * i, 5);
            }
            omote_lower_bound_inclusive = omote & 0xFF000000;
            omote_upper_bound_inclusive = omote_lower_bound_inclusive + 0x00FFFFFF;
        } else {
            omote_lower_bound_inclusive = 0;
            omote_upper_bound_inclusive = 0x0FFFFFFF;
        }
        inputIvs(ivs1, MESSAGE_01);
        ability1 = inputAbility(MESSAGE_02);
        nature1 = inputNature(MESSAGE_n1);
        inputIvs(lower, MESSAGE_l2);
        inputIvs(upper, MESSAGE_u2);
        ability2 = inputAbility(MESSAGE_i2);
        nature2 = inputNature(MESSAGE_n2);

        int flawlessIdx = -1;
        List<Integer> ivs1list = new ArrayList<>(ivs1.length);
        for (int i = 0; i < ivs1.length; i++) {
            ivs1list.add(ivs1[i]);
        }
        for (int i = 0; i < ivs1list.size();) {
            if (ivs1list.get(i) == 31) {
                flawlessIdx = i;
                ivs1list.remove(i);
            } else {
                i++;
            }
        }
        if (ivs1list.size() != 5) {
            throw new Error("threr are few or more number of 31 ivs");
        }
        System.out.println("caluclating the seed of xoroshiro rng for the 1st raid pokemon...");
        OmotePredicate.IVPredicateBuilder builder = new OmotePredicate.IVPredicateBuilder();
        builder.setFlawlessIdx(flawlessIdx).setIvs(ivs1).setNextLower(lower).setNextUpper(upper).setAbility(ability1)
                .setNextAbility(ability2).setNature(nature1).setNextnature(nature2);
        OmotePredicate predicate = builder.getIncetance();

        long start = System.currentTimeMillis();
        int[] bundle = IntStream.rangeClosed(omote_lower_bound_inclusive, omote_upper_bound_inclusive).parallel()
                .filter(predicate).toArray();
        long end = System.currentTimeMillis();
        System.out.printf("finish.[%dms]%n", end - start);
    }

    private static long readBE(int[] src, int start, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 1;
            result |= src[start + i];
        }
        return result;
    }

    static void inputIvs(int[] ivs, String message) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.println("(e.g. x x x x x x):");
            String line = scanner.nextLine();
            String[] array = line.split(" ");
            boolean validity = false;
            if (array.length == 6) {
                try {
                    for (int i = 0; i < ivs.length; i++) {
                        int iv = Integer.parseInt(array[i]);
                        validity = (iv >= 0 && iv <= 31);
                        if (validity) {
                            ivs[i] = iv;
                        } else {
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            if (validity) {
                break;
            } else {
                System.out.println("error");
            }
        }
    }

    static int inputAbility(String message) {
        int ability = 0;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.println("(0 or 1):");
            String line = scanner.nextLine();
            boolean validity = false;
            try {
                ability = Integer.parseInt(line);
                validity = (ability >= 0 && ability <= 1);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (validity) {
                break;
            } else {
                System.out.println("error");
            }
        }
        return ability;
    }

    static int inputNature(String message) {
        int ability = 0;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.println("(en):");
            String line = new String(scanner.nextLine());
            boolean validity = naturedict.containsKey(line);
            if (validity) {
                ability = naturedict.get(line);
                break;
            } else {
                System.out.println("error");
            }
        }
        return ability;
    }
}