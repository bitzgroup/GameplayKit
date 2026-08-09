package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class GKPathTest {
    @Test
    fun `exposes the points, radius, and cyclical flag it was constructed with`() {
        val points = listOf(Vector2(0f, 0f), Vector2(10f, 0f))

        val path = GKPath(points, radius = 2f, cyclical = true)

        assertEquals(points, path.points)
        assertEquals(2f, path.radius)
        assertEquals(true, path.cyclical)
    }

    @Test
    fun `cyclical defaults to false`() {
        assertFalse(GKPath(listOf(Vector2(0f, 0f)), radius = 1f).cyclical)
    }
}
