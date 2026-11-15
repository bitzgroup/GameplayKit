package jp.co.bitz.gameplaykit

/**
 * A uniform distribution of integers over `[lowestValue, highestValue]`, drawn from an underlying
 * [GKRandom] source, mirroring GameplayKit's `GKRandomDistribution`. Subclassed by
 * [GKGaussianDistribution] and [GKShuffledDistribution] for non-uniform/non-repeating variants.
 */
public open class GKRandomDistribution(
    protected val randomSource: GKRandom,
    public var lowestValue: Int,
    public var highestValue: Int,
) : GKRandom {
    /** Creates a distribution over `[lowestValue, highestValue]` backed by a fresh [GKARC4RandomSource]. */
    public constructor(lowestValue: Int, highestValue: Int) : this(GKARC4RandomSource(), lowestValue, highestValue)

    /** The number of distinct values this distribution can produce: `highestValue - lowestValue + 1`. */
    public val numberOfPossibleOutcomes: Int
        get() = highestValue - lowestValue + 1

    /** Returns a random Int in `[lowestValue, highestValue]`. */
    override fun nextInt(): Int = lowestValue + randomSource.nextInt(numberOfPossibleOutcomes)

    /**
     * Returns a uniformly random Int in `lowestValue..min(highestValue, upperBound - 1)`, drawn
     * straight from [randomSource] — matching Apple (verified on iOS/tvOS simulators), where the
     * distribution's own shape plays no part: a [GKGaussianDistribution] is uniform here too, and a
     * [GKShuffledDistribution] doesn't take a value out of its shuffled bag.
     *
     * @throws IllegalArgumentException if [upperBound] isn't above [lowestValue] (Apple raises
     *   `NSInvalidArgumentException`).
     */
    public final override fun nextInt(upperBound: Int): Int {
        require(upperBound > lowestValue) { "upperBound ($upperBound) must be above lowestValue ($lowestValue)" }
        val highest = minOf(highestValue, upperBound - 1)
        return lowestValue + randomSource.nextInt(highest - lowestValue + 1)
    }

    override fun nextUniform(): Float = randomSource.nextUniform()

    override fun nextBool(): Boolean = randomSource.nextBool()

    public companion object {
        /** Creates a distribution over `1..sideCount`, as if rolling a die with that many sides. */
        public fun forDieWithSideCount(sideCount: Int): GKRandomDistribution = GKRandomDistribution(1, sideCount)

        /** A distribution over `1..6`, as if rolling a standard six-sided die. */
        public fun d6(): GKRandomDistribution = forDieWithSideCount(6)

        /** A distribution over `1..20`, as if rolling a twenty-sided die. */
        public fun d20(): GKRandomDistribution = forDieWithSideCount(20)
    }
}
