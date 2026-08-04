package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKGaussianDistributionTest {
    @Test
    fun `range initializer derives mean and deviation, 100 percent within three deviations`() {
        // Apple's own example: three six-sided dice summed range 3...18.
        val distribution = GKGaussianDistribution(GKLinearCongruentialRandomSource(seed = 1L), 3, 18)

        assertEquals(10.5f, distribution.mean)
        assertEquals(2.5f, distribution.deviation)
    }

    @Test
    fun `mean-deviation initializer derives a range spanning six deviations`() {
        val distribution =
            GKGaussianDistribution(GKLinearCongruentialRandomSource(seed = 1L), mean = 10.5f, deviation = 2.5f)

        assertEquals(3, distribution.lowestValue)
        assertEquals(18, distribution.highestValue)
    }

    @Test
    fun `nextInt always stays within lowestValue and highestValue`() {
        val distribution = GKGaussianDistribution(GKLinearCongruentialRandomSource(seed = 2L), 3, 18)

        repeat(2000) {
            val value = distribution.nextInt()
            assertTrue(value in 3..18)
        }
    }

    @Test
    fun `samples cluster around the mean`() {
        val distribution = GKGaussianDistribution(GKLinearCongruentialRandomSource(seed = 3L), 3, 18)

        val samples = List(5000) { distribution.nextInt() }
        val average = samples.average()

        assertTrue(abs(average - 10.5) < 0.5, "expected average near 10.5 but was $average")
    }
}
