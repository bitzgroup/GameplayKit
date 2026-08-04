package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
