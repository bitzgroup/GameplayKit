package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKVoronoiNoiseSourceTest {
    @Test
    fun `distance-based sample is never negative`() {
        val source = GKVoronoiNoiseSource(frequency = 0.5, distanceEnabled = true, seed = 21)

        for (i in 0 until 100) {
            val value = source.sample(Vector3(i.toFloat(), 0f, (i * 3).toFloat()))
            assertTrue(value >= 0.0)
        }
    }

    @Test
    fun `value-based sample stays within -1 and 1`() {
        val source = GKVoronoiNoiseSource(frequency = 0.5, distanceEnabled = false, seed = 21)

        for (i in 0 until 100) {
            val value = source.sample(Vector3(i.toFloat(), 0f, (i * 3).toFloat()))
            assertTrue(value in -1.0..1.0)
        }
    }

    @Test
    fun `sample is deterministic for a fixed seed`() {
        val a = GKVoronoiNoiseSource(frequency = 0.4, seed = 8)
        val b = GKVoronoiNoiseSource(frequency = 0.4, seed = 8)

        assertEquals(a.sample(Vector3(2f, 1f, 0f)), b.sample(Vector3(2f, 1f, 0f)))
    }
}
