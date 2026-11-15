package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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

    // Expectations below are Apple's, recorded by the sample app's GameplayKitParity probes
    // (GKRandomParityTests).

    @Test
    fun `never draws the same value twice in a row, also across the wrap-around`() {
        for (high in listOf(2, 3, 6)) {
            val distribution = GKShuffledDistribution(GKLinearCongruentialRandomSource(seed = 9L), 1, high)
            val values = List(high * 2_000) { distribution.nextInt() }

            values.chunked(high).forEach { assertEquals((1..high).toSet(), it.toSet()) }
            values.zipWithNext().forEachIndexed { i, (a, b) -> assertNotEquals(a, b, "1..$high at ${i + 1}") }
        }
    }

    @Test
    fun `nextInt upperBound leaves the shuffled bag alone`() {
        val distribution = GKShuffledDistribution(GKLinearCongruentialRandomSource(seed = 11L), 1, 6)
        val bagDraws = mutableListOf<Int>()
        repeat(18) { i -> if (i % 3 == 2) distribution.nextInt(4) else bagDraws += distribution.nextInt() }

        assertEquals((1..6).toSet(), bagDraws.take(6).toSet())
        assertEquals((1..6).toSet(), bagDraws.takeLast(6).toSet())
    }
}
