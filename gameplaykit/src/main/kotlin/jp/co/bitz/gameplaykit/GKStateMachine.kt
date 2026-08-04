package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

public open class GKStateMachine(states: List<GKState>) {
    private val statesByClass = LinkedHashMap<KClass<out GKState>, GKState>()

    init {
        for (state in states) {
            statesByClass[state::class] = state
            state.stateMachine = this
        }
    }

    public open var currentState: GKState? = null
        internal set

    public open fun <T : GKState> stateForClass(stateClass: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return statesByClass[stateClass] as? T
    }

    public inline fun <reified T : GKState> state(): T? = stateForClass(T::class)

    // A machine with no currentState (before the first enter()) can always enter any registered state.
    public open fun canEnterState(stateClass: KClass<out GKState>): Boolean {
        if (stateClass !in statesByClass) return false
        return currentState?.isValidNextState(stateClass) ?: true
    }

    public inline fun <reified T : GKState> canEnterState(): Boolean = canEnterState(T::class)

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

    public inline fun <reified T : GKState> enter(): Boolean = enter(T::class)

    public open fun update(deltaTime: Duration) {
        currentState?.update(deltaTime)
    }
}
