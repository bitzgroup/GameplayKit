package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Vector2Test {
    @Test
    fun `exposes the x and y it was constructed with`() {
        val vector = Vector2(1f, 2f)

        assertEquals(1f, vector.x)
        assertEquals(2f, vector.y)
    }

    @Test
    fun `equal components produce equal vectors`() {
        assertEquals(Vector2(1f, 2f), Vector2(1f, 2f))
        assertNotEquals(Vector2(1f, 2f), Vector2(2f, 1f))
    }

    @Test
    fun `plus and minus add and subtract componentwise`() {
        assertEquals(Vector2(4f, 6f), Vector2(1f, 2f) + Vector2(3f, 4f))
        assertEquals(Vector2(-2f, -2f), Vector2(1f, 2f) - Vector2(3f, 4f))
    }

    @Test
    fun `times scales each component`() {
        assertEquals(Vector2(2f, 4f), Vector2(1f, 2f) * 2f)
    }

    @Test
    fun `dot computes the dot product`() {
        assertEquals(11f, Vector2(1f, 2f) dot Vector2(3f, 4f))
    }

    @Test
    fun `length is the Euclidean magnitude`() {
        assertEquals(5f, Vector2(3f, 4f).length())
    }

    @Test
    fun `normalized returns a unit vector in the same direction`() {
        val normalized = Vector2(3f, 4f).normalized()

        assertTrue(abs(normalized.x - 0.6f) < 0.0001f)
        assertTrue(abs(normalized.y - 0.8f) < 0.0001f)
        assertTrue(abs(normalized.length() - 1f) < 0.0001f)
    }

    @Test
    fun `normalized leaves a zero vector unchanged`() {
        assertEquals(Vector2(0f, 0f), Vector2(0f, 0f).normalized())
    }
}
