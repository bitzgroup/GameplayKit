package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
}
