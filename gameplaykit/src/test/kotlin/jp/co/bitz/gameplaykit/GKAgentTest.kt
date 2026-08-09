package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GKAgentTest {
    @Test
    fun `defaults mass to 1 and other tunables to 0`() {
        val agent = GKAgent()

        assertEquals(1f, agent.mass)
        assertEquals(0f, agent.maxAcceleration)
        assertEquals(0f, agent.maxSpeed)
        assertEquals(0f, agent.radius)
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
