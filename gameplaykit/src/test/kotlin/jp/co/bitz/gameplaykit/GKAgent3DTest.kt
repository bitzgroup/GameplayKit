package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GKAgent3DTest {
    @Test
    fun `position and rotation round-trip through their 3D setters`() {
        val agent = GKAgent3D()

        agent.position = Vector3(1f, 2f, 3f)
        agent.rotation = Vector3(1f, 0f, 0f)

        assertEquals(Vector3(1f, 2f, 3f), agent.position)
        assertEquals(Vector3(1f, 0f, 0f), agent.rotation)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated velocity setter sets rotation and speed`() {
        val agent = GKAgent3D()

        agent.velocity = Vector3(0f, 3f, 4f)

        assertEquals(5f, agent.speed, 0.0001f)
        assertEquals(0.6f, agent.rotation.y, 0.0001f)
        assertEquals(0.8f, agent.rotation.z, 0.0001f)
    }

    @Test
    fun `rightHanded defaults to true`() {
        assertTrue(GKAgent3D().rightHanded)
    }

    @Test
    fun `setting rotation to a zero vector is ignored since it has no direction`() {
        val agent = GKAgent3D()
        agent.rotation = Vector3(1f, 0f, 0f)

        agent.rotation = Vector3(0f, 0f, 0f)

        assertEquals(Vector3(1f, 0f, 0f), agent.rotation)
    }

    @Test
    fun `a seek behavior moves the agent toward the target over time`() {
        val agent = GKAgent3D()
        agent.maxSpeed = 10f
        agent.maxAcceleration = 50f
        val target = GKAgent3D()
        target.position = Vector3(0f, 0f, 100f)
        agent.behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)

        repeat(60) { agent.update(0.1.seconds) }

        assertTrue(agent.position.z > 0f)
        assertTrue(agent.position.z < 100f)
    }
}
