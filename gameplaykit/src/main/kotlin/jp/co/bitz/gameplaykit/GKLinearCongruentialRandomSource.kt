package jp.co.bitz.gameplaykit

import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * A [GKRandomSource] using a linear congruential generator, mirroring GameplayKit's
 * `GKLinearCongruentialRandomSource`. Backed directly by `java.util.Random`, which implements the
 * same 48-bit LCG algorithm (Knuth, TAOCP Vol. 2, Sec. 3.2.1, multiplier `0x5DEECE66D`) that
 * GameplayKit documents for this class — output is bit-for-bit identical to GameplayKit's.
 *
 * Fast, but not suitable for security-sensitive use (the algorithm is easily predictable).
 */
public class GKLinearCongruentialRandomSource(
    seed: Long = Random.Default.nextLong(),
) : GKRandomSource(java.util.Random(seed).asKotlinRandom()) {
    /** The seed this source was (re)initialized with. Setting it restarts the sequence from that seed. */
    public var seed: Long = seed
        set(value) {
            field = value
            random = java.util.Random(value).asKotlinRandom()
        }
}
