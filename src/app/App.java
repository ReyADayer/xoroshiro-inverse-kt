package app;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.IntStream;

public class App {
    public final static String VERSION = "1.1";
    public final static String MESSAGE_01 = "enter precise IVs of the 1st raid pokemon";
    public final static String MESSAGE_02 = "enter index of ability of the 1st raid pokemon";
    public final static String MESSAGE_s2 = "enter number of stars of 2nd raid pokemon";
    public final static String MESSAGE_l2 = "enter lower bounds of IVs of 2nd raid pokemon";
    public final static String MESSAGE_u2 = "enter upper bounds of IVs of 2nd raid pokemon";
    public final static String MESSAGE_i2 = "enter index of ability of the 2nd raid pokemon";
    public final static String MESSAGE_n1 = "enter nature of 1st raid pokemon";
    public final static String MESSAGE_n2 = "enter nature of 2nd raid pokemon";
    public final static String MESSAGE_quit = "seed(s) found. are you sure to quit now?";

    public final static Map<String, Integer> naturedict = new HashMap<>();
    static {
        String[] ja = { "がんばりや", "さみしがり", "ゆうかん", "いじっぱり", "やんちゃ", "ずぶとい", "すなお", "のんき", "わんぱく", "のうてんき", "おくびょう",
                "せっかち", "まじめ", "ようき", "むじゃき", "ひかえめ", "おっとり", "れいせい", "てれや", "うっかりや", "おだやか", "おとなしい", "なまいき", "しんちょう",
                "きまぐれ" };
        String[] en_capital = { "Hardy", "Lonely", "Brave", "Adamant", "Naughty", "Bold", "Docile", "Relaxed", "Impish",
                "Lax", "Timid", "Hasty", "Serious", "Jolly", "Naive", "Modest", "Mild", "Quiet", "Bashful", "Rash",
                "Calm", "Gentle", "Sassy", "Careful", "Quirky" };
        String[] en_small = { "hardy", "lonely", "brave", "adamant", "naughty", "bold", "docile", "relaxed", "impish",
                "lax", "timid", "hasty", "serious", "jolly", "naive", "modest", "mild", "quiet", "bashful", "rash",
                "calm", "gentle", "sassy", "careful", "quirky" };
        for (int i = 0; i < en_small.length; i++) {
            naturedict.put(ja[i], i);
            naturedict.put(en_capital[i], i);
            naturedict.put(en_small[i], i);
        }
    }

    final static Map<Integer, List<Integer>> star2flawlessIvs;
    static {
        star2flawlessIvs = new HashMap<>(5);
        star2flawlessIvs.put(1, Arrays.asList(1));
        star2flawlessIvs.put(2, Arrays.asList(1, 2));
        star2flawlessIvs.put(3, Arrays.asList(2, 3));
        star2flawlessIvs.put(4, Arrays.asList(3, 4));
        star2flawlessIvs.put(5, Arrays.asList(4));
    }

    public static void main(String[] args) throws Exception {
        System.out.printf("xoroshiro inverse %s%n", VERSION);
        System.out.println("press ctrl+c to exit");
        boolean debug = false;
        if ((args != null) && args.length == 1) {
            debug = "--debug".equals(args[0]);
        }
        search(debug);
    }

    public static boolean search(boolean debug) {
        int[] ivs1 = new int[6];
        int[] lower = new int[6];
        int[] upper = new int[6];
        int ability1, ability2;
        int omote_lower_bound_inclusive, omote_upper_bound_inclusive;
        int nature1, nature2;
        int flawlessIvs = 1;
        int maxIvRerollment;
        int stars = 1;
        System.out.printf("this program supports ONLY the cases that the 1st POKEMON's has single 31 IV%n");
        int nextStars;
        List<Integer> nextFlawlessIvsList;
        if (debug) {
            SecureRandom r = null;
            try {
                r = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if (r == null) {
                return false;
            }
            long goodseed, nextseed;
            maxIvRerollment = 0;
            nextStars = 1;
            int nextFlawlessIvs = 1;
            while (true) {
                goodseed = r.nextLong();
                if (DenGenerator.isGood(goodseed, flawlessIvs, maxIvRerollment))
                    break;
            }
            nextseed = goodseed + Xoroshiro.XOROSHIRO_CONST;
            List<Integer> g = DenGenerator.original(goodseed, flawlessIvs);
            List<Integer> h = DenGenerator.original(nextseed, nextFlawlessIvs);
            for (int i = 0; i < 6; i++) {
                ivs1[i] = g.get(i);
                lower[i] = h.get(i);
                upper[i] = h.get(i);
            }
            nextFlawlessIvsList = check(nextStars, lower, upper);
            ability1 = g.get(6);
            ability2 = h.get(6);
            nature1 = g.get(8);
            nature2 = h.get(8);
            byte[] x = DenGenerator.pure(goodseed, flawlessIvs, maxIvRerollment);
            int omote = (int) readBE(x, 0, 3);
            for (int i = 0; i < 5; i++) {
                omote = (omote << 5) | (int) readBE(x, 6 + 10 * i, 5);
            }
            omote_lower_bound_inclusive = omote & 0xFFFFFFFF;
            omote_upper_bound_inclusive = omote_lower_bound_inclusive | 0;
        } else {
            omote_lower_bound_inclusive = 0;
            omote_upper_bound_inclusive = 0x0FFFFFFF;
            while (true) {
                inputIvs(ivs1, MESSAGE_01);
                if (!check(ivs1)) {
                    System.out.println("wrong ivs");
                } else {
                    break;
                }
            }
            maxIvRerollment = 4; /* ignore 0.4% of all seeds*/
            ability1 = inputAbility(MESSAGE_02);
            nature1 = inputNature(MESSAGE_n1);
            while (true) {
                nextStars = inputStars(MESSAGE_s2);
                inputIvs(lower, MESSAGE_l2);
                inputIvs(upper, MESSAGE_u2);
                nextFlawlessIvsList = check(nextStars, lower, upper);
                if (nextFlawlessIvsList.isEmpty()) {
                    System.out.println("wrong ivs");
                } else {
                    break;
                }
            }
            ability2 = inputAbility(MESSAGE_i2);
            nature2 = inputNature(MESSAGE_n2);
        }

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
            throw new Error("there are few or more number of 31 ivs");
        }
        System.out.println("caluclating the seed of xoroshiro rng for the 1st raid pokemon...");
        long start = System.currentTimeMillis();
        List<Integer> omoList = new ArrayList<>();
        boolean quitLoop = false;
        boolean found = false;
        label_outer: for (int ivRerollment = 0; ivRerollment <= maxIvRerollment; ivRerollment++) {
            System.out.printf("suppose the number of recalculations for the 1st pokemon's IVs is %d...%n",
                    ivRerollment);
            for (int nextFlawlessIvs : nextFlawlessIvsList) {
                System.out.printf("suppose the number of ensured 31 IVs for 2nd pokemon is %d...%n", nextFlawlessIvs);
                OmotePredicate.IVPredicateBuilder builder = new OmotePredicate.IVPredicateBuilder();
                builder.setFlawlessIdx(flawlessIdx).setIvRerollment(ivRerollment).setIvs(ivs1).setNextLower(lower)
                        .setNextUpper(upper).setNextFlawlessIvs(nextFlawlessIvs).setAbility(ability1)
                        .setNextAbility(ability2).setNature(nature1).setNextnature(nature2);
                OmotePredicate predicate = builder.getIncetance();
                int[] bundle = IntStream.rangeClosed(omote_lower_bound_inclusive, omote_upper_bound_inclusive)
                        .parallel().filter(predicate).toArray();
                for (int o : bundle) {
                    omoList.add(o);
                }
                if (omoList.size() > 0) {
                    found = true;
                    if (debug) {
                        quitLoop = true;
                    } else {
                        quitLoop = inputYesNo(MESSAGE_quit);
                    }
                }
                if (quitLoop) {
                    break label_outer;
                }
            }
        }

        long end = System.currentTimeMillis();
        System.out.printf("finish.[%dms]%n", end - start);

        return found;
    }

    private static boolean check(int[] ivs1) {
        int flawlessIvs = 0;
        for (int i = 0; i < ivs1.length; i++) {
            if (ivs1[i] == 31) {
                flawlessIvs++;
            }
        }
        return flawlessIvs == 1;
    }

    private static List<Integer> check(int stars, int[] lower, int[] upper) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < lower.length; i++) {
            if (lower[i] > upper[i]) {
                return result;
            }
        }
        int naive = 0;
        int maximum = 0;
        for (int i = 0; i < lower.length; i++) {
            if (upper[i] == 31) {
                maximum++;
            }
            if (lower[i] == 31) {
                naive++;
            }
        }
        for (int programmedFlawless : star2flawlessIvs.get(stars)) {
            if (maximum >= programmedFlawless) {
                result.add(programmedFlawless);
            }
        }
        if (result.size() == 2) {
            if (result.get(1) <= naive) {
                /* reverse */
                int x = result.remove(0);
                result.add(x);
            }
        }
        return result;
    }

    private static long readBE(byte[] src, int start, int length) {
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

    static int inputStars(String message) {
        int stars = 0;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.println("(1-5):");
            String line = scanner.nextLine();
            boolean validity = false;
            try {
                stars = Integer.parseInt(line);
                validity = (stars >= 1 && stars <= 5);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (validity) {
                break;
            } else {
                System.out.println("error");
            }
        }
        return stars;
    }

    static boolean inputYesNo(String message) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.println("(y or n):");
            String line = new String(scanner.nextLine());
            if ("y".equals(line)) {
                return true;
            } else if ("n".equals(line)) {
                return false;
            } else {
                System.out.println("error");
            }
        }
    }
}