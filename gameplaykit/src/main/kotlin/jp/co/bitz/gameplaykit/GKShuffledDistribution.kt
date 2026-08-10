package jp.co.bitz.gameplaykit

/**
 * A "fair" distribution of integers over `[lowestValue, highestValue]`, mirroring GameplayKit's
 * `GKShuffledDistribution`: draws every value in the range exactly once, in a freshly shuffled
 * order, before any value repeats.
 */
public class GKShuffledDistribution(
    randomSource: GKRandom,
    lowestValue: Int,
    highestValue: Int,
) : GKRandomDistribution(randomSource, lowestValue, highestValue) {
    /** Creates a distribution over `[lowestValue, highestValue]` backed by a fresh [GKARC4RandomSource]. */
    public constructor(lowestValue: Int, highestValue: Int) : this(GKARC4RandomSource(), lowestValue, highestValue)

    private var bag: MutableList<Int> = mutableListOf()

    override fun nextInt(): Int {
        if (bag.isEmpty()) {
            bag = shuffledRange()
        }
        return bag.removeAt(bag.lastIndex)
    }

    private fun shuffledRange(): MutableList<Int> {
        val values = (lowestValue..highestValue).toMutableList()
        for (i in values.indices.reversed()) {
            val j = randomSource.nextInt(i + 1)
            val tmp = values[i]
            values[i] = values[j]
            values[j] = tmp
        }
        return values
    }

    public companion object {
        /** Creates a distribution over `1..sideCount`, as if rolling a die with that many sides. */
        public fun forDieWithSideCount(sideCount: Int): GKShuffledDistribution = GKShuffledDistribution(1, sideCount)

        /** A distribution over `1..6`, as if rolling a standard six-sided die. */
        public fun d6(): GKShuffledDistribution = forDieWithSideCount(6)

        /** A distribution over `1..20`, as if rolling a twenty-sided die. */
        public fun d20(): GKShuffledDistribution = forDieWithSideCount(20)
    }
}
