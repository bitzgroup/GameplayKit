package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GKAgentTest {
    @Test
    fun `defaults the tunables to Apple's values`() {
        val agent = GKAgent()

        assertEquals(1f, agent.mass)
        assertEquals(1f, agent.maxAcceleration)
        assertEquals(1f, agent.maxSpeed)
        assertEquals(0.5f, agent.radius)
        assertEquals(0f, agent.speed)
        assertNull(agent.behavior)
        assertNull(agent.delegate)
    }

    @Test
    fun `behavior and delegate are settable`() {
        val agent = GKAgent()
        val behavior = GKBehavior()
        val delegate = object : GKAgentDelegate {}

        agent.behavior = behavior
        agent.delegate = delegate

        assertEquals(behavior, agent.behavior)
        assertEquals(delegate, agent.delegate)
    }
}
