package xoroshiro.inverse;

import java.util.*;
import java.util.stream.IntStream;

public class App {
    public final static Map<String, Integer> natureDict = Dict.createNatureDict();
    public final static Map<String, Integer> abilityDict = Dict.createAbilityDict();
    public final static Map<String, Integer> hiddenDict = Dict.createHiddenDict();
    public final static Map<String, Integer> ECDict = Dict.createEcDict();
    final static Map<Integer, List<Integer>> star2flawlessIvs = Dict.createStar2flawlessIvs();

    public static void main(String[] args) throws Exception {
        System.out.printf("xoroshiro inverse %s%n", Messages.VERSION);
        System.out.println("press ctrl+c to exit");
        System.out.printf("this program supports ONLY the cases that the pokemons have no 31 IV in chance%n");
        boolean debug = false;
        int ivRecalculation = 0;
        if ((args != null) && args.length > 1) {
            List<String> argslist = Arrays.asList(args);
            debug = argslist.contains("--debug");
            if (argslist.contains("--IvRecalculation")) {
                int x = argslist.indexOf("--IvRecalculation");
                ivRecalculation = Integer.parseInt(argslist.get(x + 1));
            }
        }
        search(debug, ivRecalculation);
    }

    public static boolean search(boolean debug, int debugIvRecalculation) {
        return search(debug, debugIvRecalculation, 0, 0x0FFFFFFF);
    }

    public static boolean search(boolean debug, int debugIvRecalculation, int omote_lower_bound_inclusive,
                                 int omote_upper_bound_inclusive) {
        List<Integer> flawlessIvsList = new ArrayList<>();
        final List<int[]> lowersList = new ArrayList<>();
        final List<int[]> uppersList = new ArrayList<>();
        final List<Integer> abilityTypesList = new ArrayList<>();
        final List<Integer> abilitiesList = new ArrayList<>();
        final List<Boolean> requireGenderList = new ArrayList<>();
        final List<Integer> natureList = new ArrayList<>();
        final int[] flaws = new int[5];
        int minOffset, maxOffset;
        int[] ivs1 = new int[6];
        while (true) {
            inputIvs(ivs1, Messages.MESSAGE_i1);
            int flawlessIvs = countFlawlessIvs(ivs1);
            if (flawlessIvs == 0) {
                System.out.println("wrong ivs");
            } else {
                flawlessIvsList.add(flawlessIvs);
                lowersList.add(ivs1);
                uppersList.add(ivs1);
                System.out.printf("suppose the number of ensured 31 IVs for the 1st pokemon is %d...%n", flawlessIvs);
                break;
            }
        }
        if (debug) {
            minOffset = flawlessIvsList.get(0) - 1 + debugIvRecalculation;
            maxOffset = minOffset;
        } else {
            minOffset = flawlessIvsList.get(0) - 1;
            maxOffset = minOffset + 4;
        }
        List<Integer> series = new ArrayList<>();
        for (int i = 0; i < ivs1.length; i++) {
            int iv = ivs1[i];
            if (iv != 31) {
                series.add(iv);
            }
        }
        if (series.size() < 5) {
            int[] complement = new int[5 - series.size()];
            inputIvs(complement, Messages.MESSAGE_r1);
            for (int i = 0; i < complement.length; i++) {
                int r = complement[i];
                series.add(r);
            }
        }
        for (int i = 0; i < flaws.length; i++) {
            flaws[i] = series.get(i);
        }
        double information = Math.log(choose(6, flawlessIvsList.get(0))) / Math.log(2) + 5 * 5
                + Math.log(25) / Math.log(2); // iv, nature
        int num = 1;
        while (information < 64) {
            int next = num + 1;
            String ordered = order(next);
            int nextStars = inputInteger(String.format(Messages.MESSAGE_star, ordered), 1, 5);
            int[] nextLowers = new int[6];
            int[] nextUppers = new int[6];
            inputIvs(nextLowers, String.format(Messages.MESSAGE_lower, ordered));
            inputIvs(nextUppers, String.format(Messages.MESSAGE_upper, ordered));
            lowersList.add(nextLowers);
            uppersList.add(nextUppers);
            List<Integer> nextFlawlessIvsList = check(nextStars, nextLowers, nextUppers);
            if (nextFlawlessIvsList.isEmpty()) {
                System.out.println("wrong ivs");
            } else {
                num++;
                int nextFlawlessIvs = countFlawlessIvs(nextUppers);
                if (nextFlawlessIvs > 4)
                    nextFlawlessIvs = 4;
                flawlessIvsList.add(nextFlawlessIvs);
                System.out.printf("suppose the number of ensured 31 IVs for the %s pokemon is %d...%n", ordered,
                        nextFlawlessIvs);
                information += Math.log(choose(6, nextFlawlessIvs)) / Math.log(2) + 5 * (6 - nextFlawlessIvs)
                        + Math.log(25) / Math.log(2); // iv, nature
            }
        }
        for (int i = 1; i <= num; i++) {
            String ordered = order(i);
            boolean hidden = inputYesNo(String.format(Messages.MESSAGE_ability_type, ordered));
            int ability;
            if (hidden) {
                ability = inputEntry(String.format(Messages.MESSAGE_hidden_ability, ordered), hiddenDict);
            } else {
                ability = inputEntry(String.format(Messages.MESSAGE_ability, ordered), abilityDict);
            }
            boolean requireGender = inputYesNo(String.format(Messages.MESSAGE_gender, ordered));
            int nature = inputEntry(String.format(Messages.MESSAGE_nature, ordered), natureDict);
            abilityTypesList.add(hidden ? 4 : 3);
            abilitiesList.add(ability);
            requireGenderList.add(requireGender);
            natureList.add(nature);
        }

        List<Integer> probableAdditionalBits = new ArrayList<>();
        int offsetAdditionalBits;
        if (flawlessIvsList.get(0) == 1 && abilitiesList.get(0) != -1) {
            // ability of the 1st pokemon can be used
            probableAdditionalBits.add(abilitiesList.get(0));
            offsetAdditionalBits = 1;
        } else {
            int ec = inputEntry(Messages.MESSAGE_EC, ECDict);
            offsetAdditionalBits = 0;
            if (ec != -1) {
                probableAdditionalBits.add(ec);
            } else {
                probableAdditionalBits.add(0);
                probableAdditionalBits.add(1);
            }
        }

        // int flawlessIdx = -1;
        List<Integer> probableFlawlessIdx = new ArrayList<>();
        for (int i = 0; i < ivs1.length; i++) {
            if (ivs1[i] == 31)
                probableFlawlessIdx.add(i);
        }

        System.out.println("calculating the seed of xoroshiro rng for the 1st raid pokemon...");
        long start = System.currentTimeMillis();
        List<Integer> omoList = new ArrayList<>();
        boolean quitLoop = false;
        boolean found = false;
        label_outer:
        for (int offsetLastFlawlessIdx = minOffset; offsetLastFlawlessIdx <= maxOffset; offsetLastFlawlessIdx++) {
            System.out.printf("suppose the number of seeds before the last flawless IV is %d...%n",
                    offsetLastFlawlessIdx);
            for (int lastFlawlessIdx : probableFlawlessIdx) {
                System.out.printf("- suppose the index of the last flawless IV of 1st pokemon is %d...%n",
                        lastFlawlessIdx);
                for (int bit : probableAdditionalBits) {
                    OmotePredicate.IVPredicateBuilder builder = new OmotePredicate.IVPredicateBuilder();
                    builder.setFlawlessIvsList(flawlessIvsList).setLowersList(lowersList).setUppersList(uppersList)
                            .setAbilityTypesList(abilityTypesList).setAbilitiesList(abilitiesList)
                            .setRequireGenderList(requireGenderList).setNatureList(natureList)
                            .setLastFlawlessIdx(lastFlawlessIdx).setOffsetLastFlawlessIdx(offsetLastFlawlessIdx)
                            .setAdditionalBit(bit).setOffsetAdditionalBit(offsetAdditionalBits).setFlaws(flaws);
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
                            quitLoop = inputYesNo(Messages.MESSAGE_quit);
                        }
                    }
                    if (quitLoop) {
                        break label_outer;
                    }
                }
            }
        }

        long end = System.currentTimeMillis();
        System.out.printf("finish.[%dms]%n", end - start);

        return found;
    }

    private static int countFlawlessIvs(int[] ivs1) {
        int flawlessIvs = 0;
        for (int i = 0; i < ivs1.length; i++) {
            if (ivs1[i] == 31) {
                flawlessIvs++;
            }
        }
        return flawlessIvs;
    }

    public static List<Integer> check(int stars, int[] lower, int[] upper) {
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

    static void inputIvs(int[] ivs, String message) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            StringBuilder eg = new StringBuilder();
            for (int i = 0; i < ivs.length; i++) {
                eg.append(" x");
            }
            System.out.printf("(e.g.%s):%n", eg);
            String line = scanner.nextLine();
            String[] array = line.split(" ");
            boolean validity = false;
            if (array.length == ivs.length) {
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

    static int inputEntry(String message, Map<String, Integer> dictionary) {
        int result = 0;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            String line = scanner.nextLine();
            boolean validity = dictionary.containsKey(line);
            if (validity) {
                result = dictionary.get(line);
                break;
            } else {
                System.out.println("error");
            }
        }
        return result;
    }

    static int inputInteger(String message, int lower, int upper) {
        int stars = 0;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (message != null) {
                System.out.println(message);
            }
            System.out.printf("(%d-%d):%n", lower, upper);
            String line = scanner.nextLine();
            boolean validity = false;
            try {
                stars = Integer.parseInt(line);
                validity = (stars >= lower && stars <= upper);
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

    public static String order(int n) {
        if ((n % 10 == 1) && (n != 11)) {
            return String.format("%dst", n);
        } else if ((n % 10 == 2) && (n != 12)) {
            return String.format("%dnd", n);
        } else if ((n % 10 == 3) && (n != 13)) {
            return String.format("%drd", n);
        } else {
            return String.format("%dth", n);
        }
    }

    public static int choose(int n, int r) {
        int result = 1;
        for (int i = 0; i < r; i++) {
            result *= n - i;
        }
        for (int i = 0; i < r; i++) {
            result /= r - i;
        }
        return result;
    }
}