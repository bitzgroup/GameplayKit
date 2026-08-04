package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GKStateTest {
    @Test
    fun `stateMachine is null until added to a state machine`() {
        val state = IdleState()
        assertNull(state.stateMachine)
    }

    @Test
    fun `stateMachine is set once the state machine is constructed with it`() {
        val state = IdleState()
        val machine = GKStateMachine(listOf(state))

        assertEquals(machine, state.stateMachine)
    }

    @Test
    fun `isValidNextState defaults to true`() {
        val state = IdleState()

        assertTrue(state.isValidNextState(WalkState::class))
    }

    @Test
    fun `didEnter, willExit, and update are called by the state machine`() {
        val idle = IdleState()
        val walk = WalkState()
        val machine = GKStateMachine(listOf(idle, walk))

        machine.enter<IdleState>()
        machine.update(0.5.seconds)
        machine.enter<WalkState>()

        assertEquals(1, idle.didEnterCount)
        assertNull(idle.lastPreviousState)
        assertEquals(1, idle.updateCount)
        assertEquals(0.5.seconds, idle.lastDeltaTime)
        assertEquals(1, idle.willExitCount)
        assertEquals(walk, idle.lastNextState)
        assertEquals(1, walk.didEnterCount)
        assertEquals(idle, walk.lastPreviousState)
    }
}
