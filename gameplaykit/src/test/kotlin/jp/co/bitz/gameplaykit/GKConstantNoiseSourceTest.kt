package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKConstantNoiseSourceTest {
    @Test
    fun `sample always returns the configured value`() {
        val source = GKConstantNoiseSource(0.42)

        assertEquals(0.42, source.sample(Vector3(0f, 0f, 0f)))
        assertEquals(0.42, source.sample(Vector3(100f, -5f, 3f)))
    }
}
