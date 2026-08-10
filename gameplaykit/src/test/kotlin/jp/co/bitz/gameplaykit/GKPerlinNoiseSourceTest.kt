package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKPerlinNoiseSourceTest {
    @Test
    fun `sample stays within -1 and 1 across many points`() {
        val source = GKPerlinNoiseSource(frequency = 0.1, seed = 7)

        for (i in 0 until 200) {
            val value = source.sample(Vector3(i.toFloat(), (i * 2).toFloat(), (-i).toFloat()))
            assertTrue(value in -1.0..1.0)
        }
    }

    @Test
    fun `sample is deterministic for a fixed seed`() {
        val a = GKPerlinNoiseSource(frequency = 0.2, seed = 42)
        val b = GKPerlinNoiseSource(frequency = 0.2, seed = 42)

        assertEquals(a.sample(Vector3(1.5f, 2.5f, 3.5f)), b.sample(Vector3(1.5f, 2.5f, 3.5f)))
    }

    @Test
    fun `different seeds produce different fields`() {
        val a = GKPerlinNoiseSource(frequency = 0.2, seed = 1)
        val b = GKPerlinNoiseSource(frequency = 0.2, seed = 2)

        val position = Vector3(1.5f, 2.5f, 3.5f)
        assertTrue(a.sample(position) != b.sample(position))
    }

    @Test
    fun `sample is continuous at integer octave boundaries`() {
        val source = GKPerlinNoiseSource(frequency = 1.0, seed = 3)

        val justBelow = source.sample(Vector3(0.999f, 0f, 0f))
        val atBoundary = source.sample(Vector3(1.0f, 0f, 0f))
        assertTrue(kotlin.math.abs(justBelow - atBoundary) < 0.5)
    }
}
