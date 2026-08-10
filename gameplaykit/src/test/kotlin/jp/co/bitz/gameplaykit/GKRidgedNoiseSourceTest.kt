package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertTrue

class GKRidgedNoiseSourceTest {
    @Test
    fun `sample stays within -1 and 1 across many points`() {
        val source = GKRidgedNoiseSource(frequency = 0.1, seed = 11)

        for (i in 0 until 200) {
            val value = source.sample(Vector3(i.toFloat(), (i * 2).toFloat(), (-i).toFloat()))
            assertTrue(value in -1.0..1.0)
        }
    }

    @Test
    fun `sample is deterministic for a fixed seed`() {
        val a = GKRidgedNoiseSource(frequency = 0.3, seed = 5)
        val b = GKRidgedNoiseSource(frequency = 0.3, seed = 5)

        assertTrue(a.sample(Vector3(4f, 5f, 6f)) == b.sample(Vector3(4f, 5f, 6f)))
    }
}
