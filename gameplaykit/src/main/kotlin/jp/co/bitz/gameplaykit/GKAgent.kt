package jp.co.bitz.gameplaykit

import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * The base class for [GKAgent2D]/[GKAgent3D], mirroring GameplayKit's `GKAgent` (itself a
 * [GKComponent]). Movement state (position/velocity/heading) is stored here as [Vector3]
 * regardless of subclass, so the steering simulation is implemented once and shared by both
 * dimensions: `GKAgent2D` and `GKAgent3D` each expose it through their own 2D/3D-shaped
 * position/velocity/rotation properties.
 *
 * As on Apple (verified on iOS/tvOS simulators), velocity is always the heading times [speed]:
 * setting [speed] or the rotation moves the agent's velocity, and the defaults are Apple's
 * (`maxSpeed` 1, `maxAcceleration` 1, `mass` 1, `radius` 0.5).
 */
public open class GKAgent : GKComponent() {
    /** The weighted goals driving this agent's steering, or `null` for no steering force (the agent still coasts). */
    public var behavior: GKBehavior? = null

    /** Notified before/after each simulation step; see [GKAgentDelegate]. */
    public var delegate: GKAgentDelegate? = null

    /**
     * The agent's mass; steering force is divided by this to produce acceleration. `<= 0` disables
     * steering, so the agent coasts uncapped (Apple's agent positions turn NaN instead).
     */
    public var mass: Float = 1f

    /**
     * The maximum magnitude of acceleration this agent's steering force may produce. Defaults to 1,
     * as on Apple.
     */
    public var maxAcceleration: Float = 1f

    /** The maximum magnitude of this agent's velocity. Defaults to 1, as on Apple. */
    public var maxSpeed: Float = 1f

    /**
     * The agent's radius, used by separation/avoidance goals to keep agents/obstacles apart.
     * Defaults to 0.5, as on Apple.
     */
    public var radius: Float = 0.5f

    /**
     * The agent's speed along its heading. Setting it sets the velocity (heading times speed), as
     * on Apple; a negative speed is kept and points the velocity backward. Not capped at
     * [maxSpeed] until the next update, which also sets it to the new velocity's magnitude.
     */
    public var speed: Float = 0f

    internal var position3: Vector3 = Vector3(0f, 0f, 0f)

    /** The unit heading; each subclass sets Apple's default (2D: +y, 3D: +z). */
    internal var heading3: Vector3 = Vector3(1f, 0f, 0f)

    // Velocity is heading times speed; setting it sets both, keeping the heading for a zero vector.
    internal var velocity3: Vector3
        get() = heading3 * speed
        set(value) {
            val length = value.length()
            if (length > 0f) heading3 = value * (1f / length)
            speed = length
        }
}

// One simulation step shared by GKAgent2D.update and GKAgent3D.update: sums the agent's behavior's
// weighted goal forces, clamps to maxAcceleration/maxSpeed, and integrates velocity/position/heading.
internal fun stepAgent(
    agent: GKAgent,
    deltaTime: Duration,
) {
    val dt = deltaTime.toDouble(DurationUnit.SECONDS).toFloat()
    if (dt <= 0f) return

    agent.delegate?.agentWillUpdate(agent)

    // A behavior (if any) only contributes an additional steering force; an agent with no behavior
    // still coasts forward under its existing velocity, same as one whose behavior nets to zero.
    if (agent.mass > 0f) {
        val force = agent.behavior?.totalForce(agent, deltaTime) ?: Vector3(0f, 0f, 0f)
        val acceleration = force.clampedToLength(agent.maxAcceleration) * (1f / agent.mass)
        agent.velocity3 = (agent.velocity3 + acceleration * dt).clampedToLength(agent.maxSpeed)
    }
    agent.position3 += agent.velocity3 * dt

    agent.delegate?.agentDidUpdate(agent)
}

private fun Vector3.clampedToLength(maxLength: Float): Vector3 {
    if (maxLength <= 0f) return Vector3(0f, 0f, 0f)
    val len = length()
    return if (len > maxLength && len > 0f) this * (maxLength / len) else this
}
