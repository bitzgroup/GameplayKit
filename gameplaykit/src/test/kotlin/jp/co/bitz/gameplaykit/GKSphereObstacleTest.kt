package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Mirrors the tic-tac-toe sample's `GKSphereObstacleParityTests` probe: the direction each agent
 * swerves, not exact positions (Apple's steering formulas aren't the same as this library's).
 */
class GKSphereObstacleTest {
    private fun behavior(obstacle: GKObstacle): GKBehavior =
        GKBehavior.of(
            listOf(GKGoal.toAvoidObstacles(listOf(obstacle), 2.0), GKGoal.toReachTargetSpeed(5f)),
            listOf(10f, 1f),
        )

    private fun sphere(
        x: Float,
        y: Float,
        z: Float,
    ) = GKSphereObstacle(radius = 2f, position = Vector3(x, y, z))

    private fun circle(
        x: Float,
        y: Float,
    ) = GKCircleObstacle(radius = 2f, position = Vector2(x, y))

    /** A 3D agent starting at the origin heading along +z, as Apple's does by default. */
    private fun run3D(obstacle: GKObstacle): Vector3 {
        val agent =
            GKAgent3D().apply {
                maxSpeed = 5f
                maxAcceleration = 20f
                radius = 1f
                speed = 5f
                behavior = behavior(obstacle)
            }
        repeat(240) { agent.update((1.0 / 60).seconds) }
        return agent.position
    }

    private fun run2D(obstacle: GKObstacle): Vector2 {
        val agent =
            GKAgent2D().apply {
                maxSpeed = 5f
                maxAcceleration = 20f
                radius = 1f
                rotation = 0f
                speed = 5f
                behavior = behavior(obstacle)
            }
        repeat(240) { agent.update((1.0 / 60).seconds) }
        return agent.position
    }

    private fun assertStraight3D(position: Vector3) {
        assertEquals(0f, position.x, 0.01f)
        assertEquals(0f, position.y, 0.01f)
        assertEquals(20f, position.z, 0.05f)
    }

    @Test
    fun `starts at the origin and both properties can be reassigned`() {
        val sphere = GKSphereObstacle(radius = 2f)
        assertEquals(2f, sphere.radius)
        assertEquals(Vector3(0f, 0f, 0f), sphere.position)

        sphere.radius = 3f
        sphere.position = Vector3(1f, 2f, 3f)
        assertEquals(3f, sphere.radius)
        assertEquals(Vector3(1f, 2f, 3f), sphere.position)
    }

    @Test
    fun `a 3D agent swerves away from a sphere ahead of it`() {
        val offRight = run3D(sphere(0.5f, 0f, 10f))
        assertTrue(offRight.x < -5f, "$offRight")
        assertEquals(0f, offRight.y, 0.01f)
        assertTrue(offRight.z > 10f, "$offRight")

        assertTrue(run3D(sphere(-0.5f, 0f, 10f)).x > 5f)

        val offUp = run3D(sphere(0f, 0.5f, 10f))
        assertTrue(offUp.y < -5f, "$offUp")
        assertEquals(0f, offUp.x, 0.01f)
    }

    @Test
    fun `a 3D agent ignores a sphere it would clear`() {
        assertStraight3D(run3D(sphere(6f, 0f, 10f)))
        assertStraight3D(run3D(sphere(3.5f, 0f, 10f)))
        assertStraight3D(run3D(sphere(0.5f, 0f, -5f)))
    }

    @Test
    fun `a 3D agent treats a circle at x y as the sphere at x 0 y`() {
        assertTrue(run3D(circle(0.5f, 10f)).x < -5f)
        assertStraight3D(run3D(circle(10f, 0.5f)))
    }

    @Test
    fun `a 2D agent treats a sphere at x y z as the circle at x z`() {
        assertTrue(run2D(circle(10f, 0.5f)).y < -5f)
        assertTrue(run2D(sphere(10f, 0f, 0.5f)).y < -5f)
        val ignored = run2D(sphere(10f, 0.5f, 50f))
        assertEquals(20f, ignored.x, 0.05f)
        assertTrue(abs(ignored.y) < 0.01f)
    }

    @Test
    fun `moving an obstacle after an agent used it takes effect`() {
        val moved = circle(10f, 0.5f)
        run2D(moved)
        moved.position = Vector2(10f, -0.5f)
        assertTrue(run2D(moved).y > 5f)
    }
}
