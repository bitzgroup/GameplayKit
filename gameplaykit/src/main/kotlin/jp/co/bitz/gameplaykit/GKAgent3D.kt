package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * A [GKAgent] whose movement is unrestricted in three dimensions, mirroring GameplayKit's
 * `GKAgent3D`.
 *
 * Deviates from GameplayKit's `simd_quatf`-based rotation: represented here as a normalized
 * forward-direction vector rather than a full quaternion. No other API in this port needs roll
 * around the forward axis, so quaternion math wasn't warranted for that alone. It is the first
 * column of Apple's rotation matrix, which is +z for a new agent, as here.
 */
public open class GKAgent3D : GKAgent() {
    init {
        heading3 = Vector3(0f, 0f, 1f)
    }

    public var position: Vector3
        get() = position3
        set(value) {
            position3 = value
        }

    /**
     * The heading times [speed]. Apple's is read-only — set [rotation] and [speed] instead; the
     * setter is kept for compatibility and sets both.
     */
    public var velocity: Vector3
        get() = velocity3

        @Deprecated("Apple's GKAgent3D.velocity is read-only; set rotation and speed instead.")
        set(value) {
            velocity3 = value
        }

    /**
     * The agent's normalized forward direction. Setting it turns the velocity; each [update] turns
     * it to the direction of travel.
     */
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
