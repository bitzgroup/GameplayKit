package jp.co.bitz.gameplaykit

import kotlin.math.acos

// Flocking-style steering formulas (separate/align/cohere), see Steering.kt for the general design
// note shared across all Steering*.kt files.

internal fun separate(
    agent: GKAgent,
    neighbors: List<GKAgent>,
    maxDistance: Float,
    maxAngle: Float,
): Vector3 {
    var push = zero3()
    neighborsInRange(agent, neighbors, maxDistance, maxAngle).forEach { other ->
        val offset = agent.position3 - other.position3
        val distance = offset.length()
        if (distance > 0f) push += offset.normalized() * (1f / distance)
    }
    return steerToward(agent, push)
}

internal fun align(
    agent: GKAgent,
    neighbors: List<GKAgent>,
    maxDistance: Float,
    maxAngle: Float,
): Vector3 {
    val inRange = neighborsInRange(agent, neighbors, maxDistance, maxAngle)
    if (inRange.isEmpty()) return zero3()
    var averageHeading = zero3()
    inRange.forEach { averageHeading += it.velocity3 }
    return steerToward(agent, averageHeading)
}

internal fun cohere(
    agent: GKAgent,
    neighbors: List<GKAgent>,
    maxDistance: Float,
    maxAngle: Float,
): Vector3 {
    val inRange = neighborsInRange(agent, neighbors, maxDistance, maxAngle)
    if (inRange.isEmpty()) return zero3()
    var centroid = zero3()
    inRange.forEach { centroid += it.position3 }
    return seekPosition(agent, centroid * (1f / inRange.size))
}

// Converts an accumulated push/pull direction into a steering force at the agent's maxSpeed. Shared
// with SteeringAvoidance.kt's avoidAgents/avoidObstacles, which accumulate a push direction the
// same way.
internal fun steerToward(
    agent: GKAgent,
    direction: Vector3,
): Vector3 = if (direction.length() > 0f) direction.normalized() * agent.maxSpeed - agent.velocity3 else zero3()

internal fun neighborsInRange(
    agent: GKAgent,
    neighbors: List<GKAgent>,
    maxDistance: Float,
    maxAngle: Float,
): List<GKAgent> =
    neighbors.filter { other ->
        other !== agent &&
            (other.position3 - agent.position3).length() <= maxDistance &&
            withinFieldOfView(agent, other.position3, maxAngle)
    }

private fun withinFieldOfView(
    agent: GKAgent,
    otherPosition: Vector3,
    maxAngle: Float,
): Boolean {
    val heading = agent.velocity3
    val toOther = otherPosition - agent.position3
    if (heading.length() == 0f || toOther.length() == 0f) return true
    val cosAngle = (heading.normalized() dot toOther.normalized()).coerceIn(-1f, 1f)
    return acos(cosAngle) <= maxAngle
}
