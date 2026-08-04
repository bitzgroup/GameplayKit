package jp.co.bitz.gameplaykit

import kotlin.random.Random
import kotlin.random.asKotlinRandom

// Backed directly by java.util.Random, which implements the same 48-bit LCG algorithm
// (Knuth, TAOCP Vol. 2, Sec. 3.2.1, multiplier 0x5DEECE66D) that GameplayKit documents for this class.
public class GKLinearCongruentialRandomSource(
    seed: Long = Random.Default.nextLong(),
) : GKRandomSource(java.util.Random(seed).asKotlinRandom()) {
    public var seed: Long = seed
        set(value) {
            field = value
            random = java.util.Random(value).asKotlinRandom()
        }
}
