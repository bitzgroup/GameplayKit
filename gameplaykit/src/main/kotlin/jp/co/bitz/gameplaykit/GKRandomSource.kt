package jp.co.bitz.gameplaykit

import kotlin.random.Random

/**
 * A general-purpose [GKRandom] source backed by a Kotlin [Random], mirroring GameplayKit's
 * `GKRandomSource`. Used directly for platform-default randomness, or subclassed (see
 * [GKLinearCongruentialRandomSource], [GKMersenneTwisterRandomSource], [GKARC4RandomSource]) for a
 * specific, reproducible algorithm.
 */
public open class GKRandomSource internal constructor(
    internal var random: Random,
) : GKRandom {
    /** Creates a source backed by Kotlin's platform-default [Random]. */
    public constructor() : this(Random.Default)

    override fun nextInt(): Int = random.nextInt()

    override fun nextInt(upperBound: Int): Int = random.nextInt(upperBound)

    override fun nextUniform(): Float = random.nextFloat()

    override fun nextBool(): Boolean = random.nextBoolean()

    /** Returns a new list containing [list]'s elements in a random order, using this source. */
    public open fun <T> shuffled(list: List<T>): List<T> = list.shuffled(random)

    public companion object {
        private val shared: GKRandomSource by lazy { GKRandomSource() }

        /** A process-wide shared [GKRandomSource] instance, for callers that don't need their own. */
        public fun sharedRandom(): GKRandomSource = shared
    }
}
