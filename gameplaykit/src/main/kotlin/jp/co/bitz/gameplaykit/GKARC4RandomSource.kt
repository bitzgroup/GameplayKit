package jp.co.bitz.gameplaykit

import kotlin.random.Random

/**
 * A [GKRandomSource] using the ARC4 (RC4) stream cipher, mirroring GameplayKit's
 * `GKARC4RandomSource` — GameplayKit's default random source. Implements the key-scheduling
 * algorithm (KSA) followed by the pseudo-random generation algorithm (PRGA), matching
 * GameplayKit's documented algorithm.
 */
public class GKARC4RandomSource(
    seed: ByteArray = Random.Default.nextBytes(16),
) : GKRandomSource(Arc4Random(seed)) {
    private var arc4Random = random as Arc4Random

    /** The seed this source was (re)initialized with. Setting it restarts the keystream from that seed. */
    public var seed: ByteArray = seed
        set(value) {
            field = value
            arc4Random = Arc4Random(value)
            random = arc4Random
        }

    /**
     * Skips [count] values ahead in the keystream ("RC4-drop[n]"), which helps avoid RC4's known
     * statistical biases in its first output bytes.
     */
    public fun dropValues(count: Int) {
        arc4Random.drop(count)
    }
}

// The ARC4 (RC4) stream cipher: key-scheduling algorithm (KSA) followed by the
// pseudo-random generation algorithm (PRGA), matching GameplayKit's documented algorithm.
private class Arc4Random(seed: ByteArray) : Random() {
    private val s = IntArray(256) { it }
    private var i = 0
    private var j = 0

    init {
        require(seed.isNotEmpty()) { "seed must not be empty" }
        var keyIndex = 0
        for (idx in 0 until 256) {
            keyIndex = (keyIndex + s[idx] + (seed[idx % seed.size].toInt() and 0xFF)) and 0xFF
            val tmp = s[idx]
            s[idx] = s[keyIndex]
            s[keyIndex] = tmp
        }
    }

    private fun nextByte(): Int {
        i = (i + 1) and 0xFF
        j = (j + s[i]) and 0xFF
        val tmp = s[i]
        s[i] = s[j]
        s[j] = tmp
        return s[(s[i] + s[j]) and 0xFF]
    }

    fun drop(count: Int) {
        repeat(count) { nextByte() }
    }

    override fun nextBits(bitCount: Int): Int {
        var result = 0
        var collected = 0
        while (collected < bitCount) {
            result = (result shl 8) or nextByte()
            collected += 8
        }
        return result ushr (collected - bitCount)
    }
}
