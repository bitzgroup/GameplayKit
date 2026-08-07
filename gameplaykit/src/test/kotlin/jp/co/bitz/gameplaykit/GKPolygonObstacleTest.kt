package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKPolygonObstacleTest {
    @Test
    fun `exposes the vertices it was constructed with`() {
        val vertices = listOf(Vector2(0f, 0f), Vector2(1f, 0f), Vector2(0f, 1f))

        val obstacle = GKPolygonObstacle(vertices)

        assertEquals(vertices, obstacle.vertices)
    }
}
