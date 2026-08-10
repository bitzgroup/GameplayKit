package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertTrue

class GKCylindersNoiseSourceTest {
    @Test
    fun `sample stays within -1 and 1 and does not depend on y`() {
        val source = GKCylindersNoiseSource(frequency = 0.5)

        for (i in 0 until 50) {
            val value = source.sample(Vector3(i.toFloat(), 0f, (i * 2).toFloat()))
            assertTrue(value in -1.0..1.0)
        }

        val atY0 = source.sample(Vector3(3f, 0f, 4f))
        val atY10 = source.sample(Vector3(3f, 10f, 4f))
        assertTrue(atY0 == atY10)
    }
}
