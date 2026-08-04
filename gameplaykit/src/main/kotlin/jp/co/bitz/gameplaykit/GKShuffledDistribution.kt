package jp.co.bitz.gameplaykit

// "Fair" randomization: draws every value in [lowestValue, highestValue] exactly once, in a
// freshly shuffled order, before any value repeats.
public class GKShuffledDistribution(
    randomSource: GKRandom,
    lowestValue: Int,
    highestValue: Int,
) : GKRandomDistribution(randomSource, lowestValue, highestValue) {
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
        public fun forDieWithSideCount(sideCount: Int): GKShuffledDistribution = GKShuffledDistribution(1, sideCount)

        public fun d6(): GKShuffledDistribution = forDieWithSideCount(6)

        public fun d20(): GKShuffledDistribution = forDieWithSideCount(20)
    }
}
