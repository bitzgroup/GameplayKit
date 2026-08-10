package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GKCheckerboardNoiseSourceTest {
    @Test
    fun `adjacent squares alternate value`() {
        val source = GKCheckerboardNoiseSource(squareSize = 1.0)

        val a = source.sample(Vector3(0f, 0f, 0f))
        val b = source.sample(Vector3(1f, 0f, 0f))

        assertNotEquals(a, b)
    }

    @Test
    fun `points within the same square share a value`() {
        val source = GKCheckerboardNoiseSource(squareSize = 2.0)

        val a = source.sample(Vector3(0.1f, 0f, 0f))
        val b = source.sample(Vector3(1.5f, 0f, 0f))

        assertEquals(a, b)
    }
}
