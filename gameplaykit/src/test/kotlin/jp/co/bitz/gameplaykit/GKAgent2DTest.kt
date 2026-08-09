package jp.co.bitz.gameplaykit

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GKAgent2DTest {
    @Test
    fun `position velocity and rotation round-trip through their 2D setters`() {
        val agent = GKAgent2D()

        agent.position = Vector2(1f, 2f)
        agent.velocity = Vector2(3f, 4f)
        agent.rotation = (PI / 2).toFloat()

        assertEquals(Vector2(1f, 2f), agent.position)
        assertEquals(Vector2(3f, 4f), agent.velocity)
        assertTrue(abs(agent.rotation - (PI / 2).toFloat()) < 0.0001f)
    }

    @Test
    fun `a seek behavior moves the agent toward the target over time`() {
        val agent = GKAgent2D()
        agent.maxSpeed = 10f
        agent.maxAcceleration = 50f
        val target = GKAgent2D()
        target.position = Vector2(100f, 0f)
        agent.behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)

        repeat(60) { agent.update(0.1.seconds) }

        assertTrue(agent.position.x > 0f)
        assertTrue(agent.position.x < 100f)
    }

    @Test
    fun `velocity never exceeds maxSpeed`() {
        val agent = GKAgent2D()
        agent.maxSpeed = 5f
        agent.maxAcceleration = 1000f
        val target = GKAgent2D()
        target.position = Vector2(1000f, 0f)
        agent.behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)

        repeat(20) { agent.update(0.1.seconds) }

        assertTrue(agent.velocity.length() <= 5.001f)
    }

    @Test
    fun `rotation tracks the direction of travel`() {
        val agent = GKAgent2D()
        agent.maxSpeed = 10f
        agent.maxAcceleration = 50f
        val target = GKAgent2D()
        target.position = Vector2(0f, 100f)
        agent.behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)

        repeat(10) { agent.update(0.1.seconds) }

        // Moving toward +Y means a rotation angle somewhere between straight up (PI/2) and forward.
        assertTrue(agent.rotation > 0f)
    }

    @Test
    fun `delegate is notified before and after each update`() {
        val agent = GKAgent2D()
        var willUpdateCalled = false
        var didUpdateCalled = false
        agent.delegate =
            object : GKAgentDelegate {
                override fun agentWillUpdate(agent: GKAgent) {
                    willUpdateCalled = true
                }

                override fun agentDidUpdate(agent: GKAgent) {
                    didUpdateCalled = true
                }
            }

        agent.update(0.1.seconds)

        assertTrue(willUpdateCalled)
        assertTrue(didUpdateCalled)
    }
}
