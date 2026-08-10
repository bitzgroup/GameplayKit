package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * A single facet of behavior or data attached to a [GKEntity], mirroring GameplayKit's
 * `GKComponent`. Subclass this and override [update] to add per-frame logic, or
 * [didAddToEntity]/[willRemoveFromEntity] to react to attachment changes.
 */
public open class GKComponent {
    /** The entity this component is currently attached to, or `null` if unattached. Set by [GKEntity]. */
    public open var entity: GKEntity? = null
        internal set

    /** Called once per frame by [GKEntity.update]/[GKComponentSystem.update]. No-op by default. */
    public open fun update(deltaTime: Duration) {}

    /** Called after this component is attached to an entity (i.e. after [entity] is set). */
    public open fun didAddToEntity() {}

    /** Called just before this component is detached from its entity (i.e. before [entity] is cleared). */
    public open fun willRemoveFromEntity() {}
}
