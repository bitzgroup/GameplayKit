package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GKQuadTest {
    @Test
    fun `exposes the min and max it was constructed with`() {
        val quad = GKQuad(Vector2(0f, 0f), Vector2(10f, 10f))

        assertEquals(Vector2(0f, 0f), quad.min)
        assertEquals(Vector2(10f, 10f), quad.max)
    }

    @Test
    fun `equal bounds produce equal quads`() {
        val a = GKQuad(Vector2(0f, 0f), Vector2(10f, 10f))
        val b = GKQuad(Vector2(0f, 0f), Vector2(10f, 10f))
        val c = GKQuad(Vector2(0f, 0f), Vector2(5f, 5f))

        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
