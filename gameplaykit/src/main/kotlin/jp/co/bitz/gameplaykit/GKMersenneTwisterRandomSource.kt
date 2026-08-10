package jp.co.bitz.gameplaykit

import kotlin.random.Random

/**
 * A [GKRandomSource] using the Mersenne Twister algorithm, mirroring GameplayKit's
 * `GKMersenneTwisterRandomSource`. Backed by a reference MT19937-64 implementation, the same core
 * generator used by C++11's `std::mt19937_64`, matching GameplayKit's documented algorithm
 * compatibility. GameplayKit doesn't document how it derives `nextInt`/`nextUniform` from the raw
 * generator output, so exact values aren't guaranteed to be bit-identical, only the same core
 * algorithm.
 */
public class GKMersenneTwisterRandomSource(
    seed: Long = Random.Default.nextLong(),
) : GKRandomSource(MersenneTwister64(seed)) {
    /** The seed this source was (re)initialized with. Setting it restarts the sequence from that seed. */
    public var seed: Long = seed
        set(value) {
            field = value
            random = MersenneTwister64(value)
        }
}

// Reference MT19937-64 (Matsumoto & Nishimura), the same core generator used by
// C++11's std::mt19937_64, matching GameplayKit's documented algorithm compatibility.
private class MersenneTwister64(seed: Long) : Random() {
    private companion object {
        const val N = 312
        const val M = 156
        val MATRIX_A = 0xB5026F5AA96619E9UL.toLong()
        val UPPER_MASK = 0xFFFFFFFF80000000UL.toLong()
        const val LOWER_MASK = 0x7FFFFFFFL
        const val TEMPER_MASK_1 = 0x5555555555555555L
        const val TEMPER_MASK_2 = 0x71D67FFFEDA60000L
        val TEMPER_MASK_3 = 0xFFF7EEE000000000UL.toLong()
    }

    private val mt = LongArray(N)
    private var index = N + 1

    init {
        mt[0] = seed
        for (i in 1 until N) {
            mt[i] = 6364136223846793005L * (mt[i - 1] xor (mt[i - 1] ushr 62)) + i
        }
        index = N
    }

    private fun twist() {
        for (i in 0 until N) {
            val x = (mt[i] and UPPER_MASK) or (mt[(i + 1) % N] and LOWER_MASK)
            var xA = x ushr 1
            if (x and 1L != 0L) xA = xA xor MATRIX_A
            mt[i] = mt[(i + M) % N] xor xA
        }
        index = 0
    }

    private fun nextWord(): Long {
        if (index >= N) twist()
        var x = mt[index++]
        x = x xor ((x ushr 29) and TEMPER_MASK_1)
        x = x xor ((x shl 17) and TEMPER_MASK_2)
        x = x xor ((x shl 37) and TEMPER_MASK_3)
        x = x xor (x ushr 43)
        return x
    }

    override fun nextBits(bitCount: Int): Int = (nextWord() ushr (64 - bitCount)).toInt()
}
