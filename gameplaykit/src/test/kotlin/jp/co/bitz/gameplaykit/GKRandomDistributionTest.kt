package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GKRandomDistributionTest {
    @Test
    fun `nextInt always stays within lowestValue and highestValue`() {
        val distribution = GKRandomDistribution(GKLinearCongruentialRandomSource(seed = 1L), 5, 10)

        repeat(500) {
            val value = distribution.nextInt()
            assertTrue(value in 5..10)
        }
    }

    @Test
    fun `numberOfPossibleOutcomes reflects the range width`() {
        val distribution = GKRandomDistribution(1, 6)

        assertEquals(6, distribution.numberOfPossibleOutcomes)
    }

    @Test
    fun `d6 produces values from 1 to 6`() {
        val distribution = GKRandomDistribution.d6()

        assertEquals(1, distribution.lowestValue)
        assertEquals(6, distribution.highestValue)
    }

    @Test
    fun `d20 produces values from 1 to 20`() {
        val distribution = GKRandomDistribution.d20()

        assertEquals(1, distribution.lowestValue)
        assertEquals(20, distribution.highestValue)
    }

    @Test
    fun `forDieWithSideCount sets the range to 1 through sideCount`() {
        val distribution = GKRandomDistribution.forDieWithSideCount(12)

        assertEquals(1, distribution.lowestValue)
        assertEquals(12, distribution.highestValue)
    }

    @Test
    fun `nextInt upperBound never returns a value at or above the bound`() {
        val distribution = GKRandomDistribution(GKLinearCongruentialRandomSource(seed = 2L), 1, 100)

        repeat(500) {
            val value = distribution.nextInt(10)
            assertTrue(value < 10)
        }
    }

    @Test
    fun `nextUniform stays within zero and one`() {
        val distribution = GKRandomDistribution(1, 6)

        repeat(200) {
            assertTrue(distribution.nextUniform() in 0f..1f)
        }
    }

    // nextInt(upperBound:) expectations below are Apple's, recorded by the sample app's
    // GameplayKitParity probes (GKRandomParityTests).

    @Test
    fun `nextInt upperBound is uniform over lowestValue until the bound`() {
        val distribution = GKRandomDistribution(GKLinearCongruentialRandomSource(seed = 7L), 1, 6)
        val counts = (0 until 60_000).groupingBy { distribution.nextInt(3) }.eachCount()

        assertEquals(setOf(1, 2), counts.keys)
        counts.values.forEach { assertTrue(it in 28_800..31_200, "count $it") }
    }

    @Test
    fun `nextInt upperBound above the range covers the whole range`() {
        val distribution = GKRandomDistribution(GKLinearCongruentialRandomSource(seed = 7L), 1, 6)

        assertEquals((1..6).toSet(), (0 until 6_000).map { distribution.nextInt(10) }.toSet())
    }

    @Test
    fun `nextInt upperBound just above lowestValue always returns lowestValue`() {
        val distribution = GKRandomDistribution(GKLinearCongruentialRandomSource(seed = 7L), 1, 6)

        assertEquals(setOf(1), (0 until 1_000).map { distribution.nextInt(2) }.toSet())
    }

    @Test
    fun `nextInt upperBound at or below lowestValue throws`() {
        val distribution = GKRandomDistribution(5, 10)

        assertFailsWith<IllegalArgumentException> { distribution.nextInt(3) }
        assertFailsWith<IllegalArgumentException> { distribution.nextInt(5) }
    }

    @Test
    fun `a Gaussian distribution's nextInt upperBound is uniform too`() {
        val distribution = GKGaussianDistribution(GKLinearCongruentialRandomSource(seed = 5L), 1, 9)
        val counts = (0 until 60_000).groupingBy { distribution.nextInt(5) }.eachCount()

        assertEquals(setOf(1, 2, 3, 4), counts.keys)
        counts.values.forEach { assertTrue(it in 13_800..16_200, "count $it") }
    }
}
