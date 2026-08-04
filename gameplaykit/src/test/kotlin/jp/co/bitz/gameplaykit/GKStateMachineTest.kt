package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GKStateMachineTest {
    @Test
    fun `currentState is null before entering any state`() {
        val machine = GKStateMachine(listOf(IdleState()))

        assertNull(machine.currentState)
    }

    @Test
    fun `entering the first state always succeeds`() {
        val idle = IdleState()
        val machine = GKStateMachine(listOf(idle))

        assertTrue(machine.enter<IdleState>())
        assertSame(idle, machine.currentState)
    }

    @Test
    fun `enter fails for a class the machine does not know about`() {
        val machine = GKStateMachine(listOf(IdleState()))

        assertFalse(machine.enter<WalkState>())
        assertNull(machine.currentState)
    }

    @Test
    fun `canEnterState defers to the current state's isValidNextState`() {
        val restricted = RestrictedState(allowed = WalkState::class)
        val walk = WalkState()
        val jump = JumpState()
        val machine = GKStateMachine(listOf(restricted, walk, jump))
        machine.enter<RestrictedState>()

        assertTrue(machine.canEnterState<WalkState>())
        assertFalse(machine.canEnterState<JumpState>())
    }

    @Test
    fun `enter is rejected when the current state disallows the transition`() {
        val restricted = RestrictedState(allowed = WalkState::class)
        val jump = JumpState()
        val machine = GKStateMachine(listOf(restricted, jump))
        machine.enter<RestrictedState>()

        assertFalse(machine.enter<JumpState>())
        assertSame(restricted, machine.currentState)
    }

    @Test
    fun `stateForClass returns the registered instance for the exact class`() {
        val idle = IdleState()
        val machine = GKStateMachine(listOf(idle))

        assertSame(idle, machine.state<IdleState>())
        assertNull(machine.state<WalkState>())
    }
}
