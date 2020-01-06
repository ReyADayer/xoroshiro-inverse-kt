package xoroshiro.inverse

object Dict {
    private val ja = arrayOf(
        "がんばりや", "さみしがり", "ゆうかん", "いじっぱり", "やんちゃ", "ずぶとい", "すなお", "のんき", "わんぱく", "のうてんき", "おくびょう",
        "せっかち", "まじめ", "ようき", "むじゃき", "ひかえめ", "おっとり", "れいせい", "てれや", "うっかりや", "おだやか", "おとなしい", "なまいき", "しんちょう",
        "きまぐれ"
    )
    private val enCapital = arrayOf(
        "Hardy", "Lonely", "Brave", "Adamant", "Naughty", "Bold", "Docile", "Relaxed", "Impish",
        "Lax", "Timid", "Hasty", "Serious", "Jolly", "Naive", "Modest", "Mild", "Quiet", "Bashful", "Rash",
        "Calm", "Gentle", "Sassy", "Careful", "Quirky"
    )
    private val enSmall = arrayOf(
        "hardy", "lonely", "brave", "adamant", "naughty", "bold", "docile", "relaxed", "impish",
        "lax", "timid", "hasty", "serious", "jolly", "naive", "modest", "mild", "quiet", "bashful", "rash",
        "calm", "gentle", "sassy", "careful", "quirky"
    )

    @JvmStatic
    fun createNatureDict(): Map<String, Int> {
        val natureDict: MutableMap<String, Int> = mutableMapOf()

        for (i in enSmall.indices) {
            natureDict[ja[i]] = i
            natureDict[enCapital[i]] = i
            natureDict[enSmall[i]] = i
        }
        return natureDict
    }

    @JvmStatic
    fun createAbilityDict(): Map<String, Int> {
        val abilityDict: MutableMap<String, Int> = mutableMapOf()

        abilityDict["ignore"] = -1
        abilityDict["0"] = 0
        abilityDict["1"] = 1

        return abilityDict
    }

    @JvmStatic
    fun createHiddenDict(): Map<String, Int> {
        val hiddenDict: MutableMap<String, Int> = mutableMapOf()

        hiddenDict["ordinary"] = -1
        hiddenDict["0"] = 0
        hiddenDict["1"] = 1
        hiddenDict["hidden"] = 2

        return hiddenDict
    }

    @JvmStatic
    fun createEcDict(): Map<String, Int> {
        val ecDict: MutableMap<String, Int> = mutableMapOf()

        ecDict["ignore"] = -1
        ecDict["0"] = 0
        ecDict["1"] = 1

        return ecDict
    }

    @JvmStatic
    fun createStar2flawlessIvs(): Map<Int, List<Int>> {
        val star2flawlessIvs: MutableMap<Int, List<Int>> = mutableMapOf()

        star2flawlessIvs[1] = listOf(1)
        star2flawlessIvs[2] = listOf(1, 2)
        star2flawlessIvs[3] = listOf(2, 3)
        star2flawlessIvs[4] = listOf(3, 4)
        star2flawlessIvs[5] = listOf(4)

        return star2flawlessIvs
    }
}