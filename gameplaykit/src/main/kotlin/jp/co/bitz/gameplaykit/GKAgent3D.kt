package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * A [GKAgent] whose movement is unrestricted in three dimensions, mirroring GameplayKit's
 * `GKAgent3D`.
 *
 * Deviates from GameplayKit's `simd_quatf`-based rotation: represented here as a normalized
 * forward-direction vector rather than a full quaternion. No other API in this port needs roll
 * around the forward axis, so quaternion math wasn't warranted for that alone.
 */
public open class GKAgent3D : GKAgent() {
    public var position: Vector3
        get() = position3
        set(value) {
            position3 = value
        }

    public var velocity: Vector3
        get() = velocity3
        set(value) {
            velocity3 = value
        }

    /** The agent's normalized forward-direction; kept in sync with velocity by each [update]. */
    public var rotation: Vector3
        get() = heading3
        set(value) {
            heading3 = if (value.length() > 0f) value.normalized() else heading3
        }

    public var rightHanded: Boolean = true

    override fun update(deltaTime: Duration) {
        stepAgent(this, deltaTime)
    }
}
