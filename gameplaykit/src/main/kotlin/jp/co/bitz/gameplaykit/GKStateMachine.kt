package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Manages transitions between a fixed set of [GKState]s, mirroring GameplayKit's `GKStateMachine`.
 * Holds at most one state per concrete state class; [currentState] is `null` until [enter] is
 * called for the first time.
 */
public open class GKStateMachine(states: List<GKState>) {
    private val statesByClass = LinkedHashMap<KClass<out GKState>, GKState>()

    init {
        for (state in states) {
            statesByClass[state::class] = state
            state.stateMachine = this
        }
    }

    /** The state this machine is currently in, or `null` before the first call to [enter]. */
    public open var currentState: GKState? = null
        internal set

    /** Returns the registered state whose concrete class is exactly [stateClass], or `null` if none is registered. */
    public open fun <T : GKState> stateForClass(stateClass: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return statesByClass[stateClass] as? T
    }

    /** Reified convenience for [stateForClass]. */
    public inline fun <reified T : GKState> state(): T? = stateForClass(T::class)

    /**
     * Whether [enter] would currently succeed for [stateClass]: `stateClass` must be registered,
     * and either there is no [currentState] yet, or [GKState.isValidNextState] on `currentState`
     * allows the transition. A machine with no `currentState` (before the first `enter()`) can
     * always enter any registered state.
     */
    public open fun canEnterState(stateClass: KClass<out GKState>): Boolean {
        if (stateClass !in statesByClass) return false
        return currentState?.isValidNextState(stateClass) ?: true
    }

    /** Reified convenience for [canEnterState]. */
    public inline fun <reified T : GKState> canEnterState(): Boolean = canEnterState(T::class)

    /**
     * Attempts to transition to the registered state whose class is [stateClass]. If
     * [canEnterState] allows it, calls [GKState.willExit] on the current state (if any),
     * updates [currentState], then calls [GKState.didEnter] on the new state, and returns `true`.
     * Otherwise leaves the machine unchanged and returns `false`.
     */
    public open fun enter(stateClass: KClass<out GKState>): Boolean {
        val nextState = statesByClass[stateClass]
        return if (nextState != null && canEnterState(stateClass)) {
            val previousState = currentState
            previousState?.willExit(nextState)
            currentState = nextState
            nextState.didEnter(previousState)
            true
        } else {
            false
        }
    }

    /** Reified convenience for [enter]. */
    public inline fun <reified T : GKState> enter(): Boolean = enter(T::class)

    /** Calls [GKState.update] on [currentState], if any. */
    public open fun update(deltaTime: Duration) {
        currentState?.update(deltaTime)
    }
}
