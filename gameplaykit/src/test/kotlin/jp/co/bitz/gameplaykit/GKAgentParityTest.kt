package jp.co.bitz.gameplaykit

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Mirrors the tic-tac-toe sample's `GKAgentParityTests` probe of Apple's agent defaults and speed. */
class GKAgentParityTest {
    private fun assertVector(
        expected: Vector2,
        actual: Vector2,
    ) {
        assertEquals(expected.x, actual.x, 1e-5f, "$actual")
        assertEquals(expected.y, actual.y, 1e-5f, "$actual")
    }

    private fun assertVector(
        expected: Vector3,
        actual: Vector3,
    ) {
        assertEquals(expected.x, actual.x, 1e-5f, "$actual")
        assertEquals(expected.y, actual.y, 1e-5f, "$actual")
        assertEquals(expected.z, actual.z, 1e-5f, "$actual")
    }

    @Test
    fun `agent defaults`() {
        for (agent in listOf(GKAgent2D(), GKAgent3D())) {
            assertEquals(0f, agent.speed)
            assertEquals(1f, agent.maxSpeed)
            assertEquals(1f, agent.maxAcceleration)
            assertEquals(1f, agent.mass)
            assertEquals(0.5f, agent.radius)
        }
        assertEquals((PI / 2).toFloat(), GKAgent2D().rotation, 1e-6f)
        assertVector(Vector2(0f, 0f), GKAgent2D().velocity)
        assertVector(Vector3(0f, 0f, 0f), GKAgent3D().velocity)
    }

    @Test
    fun `a 2D agent's velocity is its heading times speed`() {
        val agent = GKAgent2D()
        agent.speed = 3f
        assertVector(Vector2(0f, 3f), agent.velocity)
        agent.rotation = 0f
        assertVector(Vector2(3f, 0f), agent.velocity)
        assertEquals(3f, agent.speed)
    }

    @Test
    fun `a negative speed is kept and points velocity backward`() {
        val agent = GKAgent2D()
        agent.rotation = 0f
        agent.speed = -2f
        assertEquals(-2f, agent.speed)
        assertVector(Vector2(-2f, 0f), agent.velocity)
        assertEquals(0f, agent.rotation)
    }

    @Test
    fun `a zero speed keeps the rotation`() {
        val agent = GKAgent2D()
        agent.rotation = 1f
        agent.speed = 0f
        assertVector(Vector2(0f, 0f), agent.velocity)
        assertEquals(1f, agent.rotation, 1e-6f)

        val still =
            GKAgent2D().apply {
                rotation = 0.5f
                maxSpeed = 5f
            }
        still.update(1.seconds)
        assertEquals(0.5f, still.rotation, 1e-6f)
        assertVector(Vector2(0f, 0f), still.position)
    }

    @Test
    fun `a 2D rotation is wrapped into zero to two pi`() {
        val agent = GKAgent2D()
        agent.rotation = 7f
        assertEquals(7f - 2 * PI.toFloat(), agent.rotation, 1e-5f)
        agent.rotation = -1f
        assertEquals(2 * PI.toFloat() - 1f, agent.rotation, 1e-5f)
    }

    @Test
    fun `a coasting agent moves at its speed capped by maxSpeed`() {
        val capped =
            GKAgent2D().apply {
                rotation = 0f
                maxSpeed = 2f
                speed = 5f
            }
        assertEquals(5f, capped.speed)
        capped.update(1.seconds)
        assertVector(Vector2(2f, 0f), capped.position)
        assertEquals(2f, capped.speed, 1e-5f)

        val free =
            GKAgent2D().apply {
                rotation = 0f
                maxSpeed = 10f
                speed = 5f
            }
        free.update(1.seconds)
        assertVector(Vector2(5f, 0f), free.position)

        val byDefault =
            GKAgent2D().apply {
                rotation = 0f
                speed = 5f
            }
        byDefault.update(1.seconds)
        assertVector(Vector2(1f, 0f), byDefault.position)
    }

    @Test
    fun `steering keeps speed and rotation in step with velocity`() {
        val target = GKAgent2D().apply { position = Vector2(0f, 10f) }
        val agent =
            GKAgent2D().apply {
                rotation = 0f
                maxSpeed = 5f
                maxAcceleration = 100f
                speed = 1f
                behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)
            }
        repeat(10) { agent.update(0.05.seconds) }
        assertEquals(agent.velocity.length(), agent.speed, 1e-4f)
        assertEquals(atan2(agent.velocity.y, agent.velocity.x), agent.rotation, 1e-4f)
        assertTrue(agent.position.y > agent.position.x)
    }

    @Test
    fun `a 3D agent heads along plus z and its velocity is heading times speed`() {
        val agent = GKAgent3D()
        assertVector(Vector3(0f, 0f, 1f), agent.rotation)
        agent.speed = 3f
        assertVector(Vector3(0f, 0f, 3f), agent.velocity)
        agent.rotation = Vector3(1f, 0f, 0f)
        assertVector(Vector3(3f, 0f, 0f), agent.velocity)
        agent.speed = -1f
        assertVector(Vector3(-1f, 0f, 0f), agent.velocity)
    }

    @Test
    fun `rightHanded does not change the default heading`() {
        val agent = GKAgent3D()
        agent.rightHanded = false
        agent.speed = 1f
        assertVector(Vector3(0f, 0f, 1f), agent.velocity)
    }

    @Test
    fun `a 3D agent coasts along z at its default maxSpeed`() {
        val agent = GKAgent3D().apply { speed = 5f }
        agent.update(1.seconds)
        assertVector(Vector3(0f, 0f, 1f), agent.position)
        assertEquals(1f, agent.speed, 1e-5f)
    }
}
