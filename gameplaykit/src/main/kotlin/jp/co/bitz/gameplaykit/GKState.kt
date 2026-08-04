package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

public abstract class GKState {
    public open var stateMachine: GKStateMachine? = null
        internal set

    public open fun isValidNextState(stateClass: KClass<out GKState>): Boolean = true

    public open fun didEnter(previousState: GKState?) {}

    public open fun update(deltaTime: Duration) {}

    public open fun willExit(nextState: GKState) {}
}
