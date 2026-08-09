package jp.co.bitz.gameplaykit

import kotlin.time.Duration
import kotlin.time.DurationUnit

// The base class for GKAgent2D/GKAgent3D, mirroring GameplayKit's GKAgent (itself a GKComponent).
// Movement state (position/velocity/heading) is stored here as Vector3 regardless of subclass, so
// the steering simulation is implemented once and shared by both dimensions: GKAgent2D and
// GKAgent3D each expose it through their own 2D/3D-shaped position/velocity/rotation properties.
public open class GKAgent : GKComponent() {
    public var behavior: GKBehavior? = null
    public var delegate: GKAgentDelegate? = null
    public var mass: Float = 1f
    public var maxAcceleration: Float = 0f
    public var maxSpeed: Float = 0f
    public var radius: Float = 0f
    public var speed: Float = 0f

    internal var position3: Vector3 = Vector3(0f, 0f, 0f)
    internal var velocity3: Vector3 = Vector3(0f, 0f, 0f)
    internal var heading3: Vector3 = Vector3(1f, 0f, 0f)
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
    agent.speed = agent.velocity3.length()
    if (agent.velocity3.length() > 0f) agent.heading3 = agent.velocity3.normalized()

    agent.delegate?.agentDidUpdate(agent)
}

private fun Vector3.clampedToLength(maxLength: Float): Vector3 {
    if (maxLength <= 0f) return Vector3(0f, 0f, 0f)
    val len = length()
    return if (len > maxLength && len > 0f) this * (maxLength / len) else this
}
