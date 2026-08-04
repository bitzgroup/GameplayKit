package jp.co.bitz.gameplaykit

import kotlin.reflect.KClass
import kotlin.time.Duration

internal open class TrackingState : GKState() {
    var didEnterCount = 0
        private set
    var lastPreviousState: GKState? = null
        private set
    var willExitCount = 0
        private set
    var lastNextState: GKState? = null
        private set
    var updateCount = 0
        private set
    var lastDeltaTime: Duration = Duration.ZERO
        private set

    override fun didEnter(previousState: GKState?) {
        didEnterCount++
        lastPreviousState = previousState
    }

    override fun willExit(nextState: GKState) {
        willExitCount++
        lastNextState = nextState
    }

    override fun update(deltaTime: Duration) {
        updateCount++
        lastDeltaTime = deltaTime
    }
}

internal class IdleState : TrackingState()

internal class WalkState : TrackingState()

internal class JumpState : TrackingState()

internal class RestrictedState(private val allowed: KClass<out GKState>) : TrackingState() {
    override fun isValidNextState(stateClass: KClass<out GKState>): Boolean = stateClass == allowed
}
