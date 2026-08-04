package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKShuffledDistributionTest {
    @Test
    fun `every value appears exactly once per full cycle`() {
        val distribution = GKShuffledDistribution(GKLinearCongruentialRandomSource(seed = 1L), 1, 6)

        val firstCycle = List(6) { distribution.nextInt() }
        val secondCycle = List(6) { distribution.nextInt() }

        assertEquals((1..6).toList(), firstCycle.sorted())
        assertEquals((1..6).toList(), secondCycle.sorted())
    }

    @Test
    fun `d6 factory produces a GKShuffledDistribution ranging 1 to 6`() {
        val distribution = GKShuffledDistribution.d6()

        assertEquals(1, distribution.lowestValue)
        assertEquals(6, distribution.highestValue)
    }

    @Test
    fun `d20 factory produces a GKShuffledDistribution ranging 1 to 20`() {
        val distribution = GKShuffledDistribution.d20()

        assertEquals(1, distribution.lowestValue)
        assertEquals(20, distribution.highestValue)
    }

    @Test
    fun `forDieWithSideCount sets the range to 1 through sideCount`() {
        val distribution = GKShuffledDistribution.forDieWithSideCount(10)

        assertEquals(1, distribution.lowestValue)
        assertEquals(10, distribution.highestValue)
    }
}
