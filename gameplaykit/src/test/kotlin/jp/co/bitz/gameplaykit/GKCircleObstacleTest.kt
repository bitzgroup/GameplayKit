package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKCircleObstacleTest {
    @Test
    fun `defaults to the origin and exposes the given radius`() {
        val obstacle = GKCircleObstacle(radius = 5f)

        assertEquals(5f, obstacle.radius)
        assertEquals(Vector2(0f, 0f), obstacle.position)
    }

    @Test
    fun `radius and position can be reassigned`() {
        val obstacle = GKCircleObstacle(radius = 5f)

        obstacle.radius = 10f
        obstacle.position = Vector2(1f, 2f)

        assertEquals(10f, obstacle.radius)
        assertEquals(Vector2(1f, 2f), obstacle.position)
    }
}
