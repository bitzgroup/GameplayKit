package jp.co.bitz.gameplaykit

import kotlin.time.Duration

public open class GKComponent {
    public open var entity: GKEntity? = null
        internal set

    public open fun update(deltaTime: Duration) {}

    public open fun didAddToEntity() {}

    public open fun willRemoveFromEntity() {}
}
