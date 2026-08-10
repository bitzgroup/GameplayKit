package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertTrue

class GKSpheresNoiseSourceTest {
    @Test
    fun `sample stays within -1 and 1`() {
        val source = GKSpheresNoiseSource(frequency = 0.5)

        for (i in 0 until 50) {
            val value = source.sample(Vector3(i.toFloat(), (-i).toFloat(), (i * 2).toFloat()))
            assertTrue(value in -1.0..1.0)
        }
    }

    @Test
    fun `the origin is the center of a shell`() {
        val source = GKSpheresNoiseSource(frequency = 1.0)

        assertTrue(source.sample(Vector3(0f, 0f, 0f)) == 1.0)
    }
}
