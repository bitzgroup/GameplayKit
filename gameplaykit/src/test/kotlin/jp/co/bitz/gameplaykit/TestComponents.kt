package jp.co.bitz.gameplaykit

import kotlin.time.Duration

internal open class BaseComponent : GKComponent()

internal class SubComponent : BaseComponent()

internal class MovementComponent : GKComponent() {
    var updateCount = 0

    override fun update(deltaTime: Duration) {
        updateCount++
    }
}

internal class HealthComponent : GKComponent() {
    var updateCount = 0

    override fun update(deltaTime: Duration) {
        updateCount++
    }
}

internal class RecordingComponent : GKComponent() {
    var updateCount = 0
    var lastDeltaTime = Duration.ZERO
    var addedToEntity = false
    var removedFromEntity = false

    override fun update(deltaTime: Duration) {
        updateCount++
        lastDeltaTime = deltaTime
    }

    override fun didAddToEntity() {
        addedToEntity = true
    }

    override fun willRemoveFromEntity() {
        removedFromEntity = true
    }
}
