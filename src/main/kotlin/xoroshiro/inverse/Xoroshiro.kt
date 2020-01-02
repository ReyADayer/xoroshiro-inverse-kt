package xoroshiro.inverse

class Xoroshiro {
    @JvmField
    val s = longArrayOf(0, 0)
    @JvmField
    var i: Long = 0

    constructor(seed: Long) {
        s[0] = seed
        s[1] = XOROSHIRO_CONST
    }

    constructor(s0: Long, s1: Long) {
        s[0] = s0
        s[1] = s1
    }

    operator fun next(): Long {
        val s0 = s[0]
        var s1 = s[1]
        val result = s0 + s1
        s1 = s1 xor s0
        s[0] = rotl(s0, 24) xor s1 xor (s1 shl 16)
        s[1] = rotl(s1, 37)
        i++
        return result
    }

    @JvmOverloads
    fun nextInt(mod: Long = 0xFFFFFFFFL): Long {
        var res: Long = 0
        val p2mod = nextP2(mod)
        do {
            res = next() and p2mod
        } while (res >= mod)
        return res
    }

    companion object {
        const val XOROSHIRO_CONST = -0x7d5d4e8add6295a5L
        fun rotl(x: Long, k: Int): Long {
            return x shl k or (x ushr 64 - k)
        }

        private fun nextP2(n: Long): Long {
            var n = n
            n--
            for (i in 0..5) {
                n = n or n ushr (1 shl i)
            }
            return n
        }
    }
}