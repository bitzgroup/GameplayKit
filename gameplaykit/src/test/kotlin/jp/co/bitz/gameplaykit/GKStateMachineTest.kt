package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

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

    // A deliberate deviation: Apple's machine reports success here and drops currentState to null
    // (see docs/API_COMPATIBILITY.md).
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

    private class LoggingState(
        val log: MutableList<String>,
    ) : GKState() {
        override fun didEnter(previousState: GKState?) {
            log += "didEnter from ${previousState?.let { it::class.simpleName }}"
        }

        override fun willExit(nextState: GKState) {
            log += "willExit to ${nextState::class.simpleName}"
        }

        override fun update(deltaTime: kotlin.time.Duration) {
            log += "update"
        }
    }

    // Expectations below are Apple's, recorded by the sample app's GameplayKitParity probes
    // (GKStateMachineParityTests).

    @Test
    fun `update with no current state does nothing`() {
        val log = mutableListOf<String>()
        val machine = GKStateMachine(listOf(LoggingState(log)))

        machine.update(100.milliseconds)

        assertNull(machine.currentState)
        assertEquals(emptyList(), log)
    }

    @Test
    fun `entering the current state runs a full exit and enter`() {
        val log = mutableListOf<String>()
        val machine = GKStateMachine(listOf(LoggingState(log)))
        machine.enter(LoggingState::class)
        log.clear()

        assertTrue(machine.canEnterState(LoggingState::class))
        assertTrue(machine.enter(LoggingState::class))
        assertEquals(listOf("willExit to LoggingState", "didEnter from LoggingState"), log)
    }
}
