package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Vector3Test {
    @Test
    fun `exposes the x, y, and z it was constructed with`() {
        val vector = Vector3(1f, 2f, 3f)

        assertEquals(1f, vector.x)
        assertEquals(2f, vector.y)
        assertEquals(3f, vector.z)
    }

    @Test
    fun `equal components produce equal vectors`() {
        assertEquals(Vector3(1f, 2f, 3f), Vector3(1f, 2f, 3f))
        assertNotEquals(Vector3(1f, 2f, 3f), Vector3(3f, 2f, 1f))
    }

    @Test
    fun `plus and minus add and subtract componentwise`() {
        assertEquals(Vector3(5f, 7f, 9f), Vector3(1f, 2f, 3f) + Vector3(4f, 5f, 6f))
        assertEquals(Vector3(-3f, -3f, -3f), Vector3(1f, 2f, 3f) - Vector3(4f, 5f, 6f))
    }

    @Test
    fun `times scales each component`() {
        assertEquals(Vector3(2f, 4f, 6f), Vector3(1f, 2f, 3f) * 2f)
    }

    @Test
    fun `dot computes the dot product`() {
        assertEquals(32f, Vector3(1f, 2f, 3f) dot Vector3(4f, 5f, 6f))
    }

    @Test
    fun `length is the Euclidean magnitude`() {
        assertEquals(7f, Vector3(2f, 3f, 6f).length())
    }

    @Test
    fun `normalized returns a unit vector in the same direction`() {
        val normalized = Vector3(2f, 3f, 6f).normalized()

        assertTrue(abs(normalized.x - 2f / 7f) < 0.0001f)
        assertTrue(abs(normalized.y - 3f / 7f) < 0.0001f)
        assertTrue(abs(normalized.z - 6f / 7f) < 0.0001f)
        assertTrue(abs(normalized.length() - 1f) < 0.0001f)
    }

    @Test
    fun `normalized leaves a zero vector unchanged`() {
        assertEquals(Vector3(0f, 0f, 0f), Vector3(0f, 0f, 0f).normalized())
    }
}
