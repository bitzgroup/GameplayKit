package jp.co.bitz.gameplaykit

/**
 * The common contract for a source of randomness, mirroring GameplayKit's `GKRandom` protocol.
 * Implemented by [GKRandomSource] (and its subclasses) and by [GKRandomDistribution].
 */
public interface GKRandom {
    /** Returns a random Int spanning the full Int range. */
    public fun nextInt(): Int

    /** Returns a random Int in `0 until upperBound`. */
    public fun nextInt(upperBound: Int): Int

    /** Returns a random Float in `[0, 1)`. */
    public fun nextUniform(): Float

    /** Returns a random Boolean. */
    public fun nextBool(): Boolean
}
