package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
}
