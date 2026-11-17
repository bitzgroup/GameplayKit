package jp.co.bitz.gameplaykit

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

/**
 * A [GKAgent] whose movement is restricted to two dimensions, mirroring GameplayKit's `GKAgent2D`.
 * A new agent heads along +y ([rotation] π/2), as Apple's does.
 */
public open class GKAgent2D : GKAgent() {
    init {
        heading3 = Vector3(0f, 1f, 0f)
    }

    public var position: Vector2
        get() = Vector2(position3.x, position3.y)
        set(value) {
            position3 = Vector3(value.x, value.y, 0f)
        }

    /**
     * The heading times [speed]. Apple's is read-only — set [rotation] and [speed] instead; the
     * setter is kept for compatibility and sets both.
     */
    public var velocity: Vector2
        get() = Vector2(velocity3.x, velocity3.y)

        @Deprecated("Apple's GKAgent2D.velocity is read-only; set rotation and speed instead.")
        set(value) {
            velocity3 = Vector3(value.x, value.y, 0f)
        }

    /**
     * Angle (radians) of the agent's heading, in `0 until 2π` as on Apple (a set value is wrapped).
     * Setting it turns the velocity; each [update] turns it to the direction of travel.
     */
    public var rotation: Float
        get() {
            val angle = atan2(heading3.y, heading3.x)
            return if (angle < 0f) angle + (2 * PI).toFloat() else angle
        }
        set(value) {
            heading3 = Vector3(cos(value), sin(value), 0f)
        }

    override fun update(deltaTime: Duration) {
        stepAgent(this, deltaTime)
    }
}
