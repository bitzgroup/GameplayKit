package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A single state within a [GKStateMachine], mirroring GameplayKit's `GKState`. Subclass this and
 * override the lifecycle callbacks ([didEnter], [update], [willExit]) to implement per-state
 * behavior, and [isValidNextState] to restrict which states can follow this one.
 */
public abstract class GKState {
    /** The state machine this state is registered with, or `null` if unregistered. Set by [GKStateMachine]. */
    public open var stateMachine: GKStateMachine? = null
        internal set

    /** Whether the state machine may transition from this state to [stateClass]. Allows any state by default. */
    public open fun isValidNextState(stateClass: KClass<out GKState>): Boolean = true

    /** Called when the state machine enters this state, with the state it was previously in (if any). */
    public open fun didEnter(previousState: GKState?) {}

    /** Called once per frame while this is the state machine's current state. No-op by default. */
    public open fun update(deltaTime: Duration) {}

    /** Called just before the state machine transitions away from this state, to [nextState]. */
    public open fun willExit(nextState: GKState) {}
}
