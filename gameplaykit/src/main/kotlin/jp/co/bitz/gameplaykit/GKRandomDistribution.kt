package jp.co.bitz.gameplaykit

public open class GKRandomDistribution(
    protected val randomSource: GKRandom,
    public var lowestValue: Int,
    public var highestValue: Int,
) : GKRandom {
    public constructor(lowestValue: Int, highestValue: Int) : this(GKARC4RandomSource(), lowestValue, highestValue)

    public val numberOfPossibleOutcomes: Int
        get() = highestValue - lowestValue + 1

    override fun nextInt(): Int = lowestValue + randomSource.nextInt(numberOfPossibleOutcomes)

    // Interpreted consistently across all distribution subtypes as "the distribution's own
    // nextInt(), capped below upperBound" since GameplayKit doesn't document this combination precisely.
    public final override fun nextInt(upperBound: Int): Int = nextInt().coerceAtMost(upperBound - 1)

    override fun nextUniform(): Float = randomSource.nextUniform()

    override fun nextBool(): Boolean = randomSource.nextBool()

    public companion object {
        public fun forDieWithSideCount(sideCount: Int): GKRandomDistribution = GKRandomDistribution(1, sideCount)

        public fun d6(): GKRandomDistribution = forDieWithSideCount(6)

        public fun d20(): GKRandomDistribution = forDieWithSideCount(20)
    }
}
