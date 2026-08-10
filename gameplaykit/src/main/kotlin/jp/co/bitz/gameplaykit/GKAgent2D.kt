package jp.co.bitz.gameplaykit

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

/** A [GKAgent] whose movement is restricted to two dimensions, mirroring GameplayKit's `GKAgent2D`. */
public open class GKAgent2D : GKAgent() {
    public var position: Vector2
        get() = Vector2(position3.x, position3.y)
        set(value) {
            position3 = Vector3(value.x, value.y, 0f)
        }

    public var velocity: Vector2
        get() = Vector2(velocity3.x, velocity3.y)
        set(value) {
            velocity3 = Vector3(value.x, value.y, 0f)
        }

    /** Angle (radians) of the agent's current heading; kept in sync with velocity by each [update]. */
    public var rotation: Float
        get() = atan2(heading3.y, heading3.x)
        set(value) {
            heading3 = Vector3(cos(value), sin(value), 0f)
        }

    override fun update(deltaTime: Duration) {
        stepAgent(this, deltaTime)
    }
}
