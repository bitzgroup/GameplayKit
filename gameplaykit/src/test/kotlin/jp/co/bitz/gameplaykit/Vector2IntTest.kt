package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Vector2IntTest {
    @Test
    fun `exposes the x and y it was constructed with`() {
        val vector = Vector2Int(1, 2)

        assertEquals(1, vector.x)
        assertEquals(2, vector.y)
    }

    @Test
    fun `equal components produce equal vectors`() {
        assertEquals(Vector2Int(1, 2), Vector2Int(1, 2))
        assertNotEquals(Vector2Int(1, 2), Vector2Int(2, 1))
    }
}
