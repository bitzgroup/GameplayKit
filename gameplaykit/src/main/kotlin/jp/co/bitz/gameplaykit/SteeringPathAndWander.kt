package jp.co.bitz.gameplaykit

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val WANDER_CIRCLE_DISTANCE = 1f
private const val WANDER_CIRCLE_RADIUS = 1f
private const val WANDER_ANGLE_DELTA = 0.5f

// Path-following and wander steering formulas, see Steering.kt for the general design note shared
// across all Steering*.kt files. Path following operates in the agent's XY plane (see GKPath).

internal fun followPath(
    agent: GKAgent,
    path: GKPath,
    maxPredictionTime: Double,
    forward: Boolean,
): Vector3 {
    if (path.points.size < 2) return zero3()
    val dt = maxPredictionTime.toFloat()
    val predicted2D = Vector2(agent.position3.x + agent.velocity3.x * dt, agent.position3.y + agent.velocity3.y * dt)
    val nearest = nearestPointOnPath(path, predicted2D)
    val step = if (forward) 1 else -1
    val target = path.points[(nearest.segmentIndex + step).mod(path.points.size)]
    return seekPosition(agent, Vector3(target.x, target.y, agent.position3.z))
}

internal fun stayOnPath(
    agent: GKAgent,
    path: GKPath,
    maxPredictionTime: Double,
): Vector3 {
    if (path.points.isEmpty()) return zero3()
    val dt = maxPredictionTime.toFloat()
    val predicted2D = Vector2(agent.position3.x + agent.velocity3.x * dt, agent.position3.y + agent.velocity3.y * dt)
    val nearest = nearestPointOnPath(path, predicted2D)
    return if (nearest.distance <= path.radius) {
        zero3()
    } else {
        seekPosition(agent, Vector3(nearest.point.x, nearest.point.y, agent.position3.z))
    }
}

internal fun wander(
    agent: GKAgent,
    speed: Float,
    wanderAngle: Float,
): Vector3 {
    val heading = if (agent.heading3.length() > 0f) agent.heading3.normalized() else Vector3(1f, 0f, 0f)
    val circleCenter = agent.position3 + heading * WANDER_CIRCLE_DISTANCE
    val displacement = Vector3(cos(wanderAngle), sin(wanderAngle), 0f) * WANDER_CIRCLE_RADIUS
    val toTarget = (circleCenter + displacement) - agent.position3
    val desired = if (toTarget.length() > 0f) toTarget.normalized() * speed else zero3()
    return desired - agent.velocity3
}

internal fun nextWanderAngle(previous: Float): Float = previous + (Random.nextFloat() - 0.5f) * WANDER_ANGLE_DELTA

private data class NearestPathPoint(val point: Vector2, val distance: Float, val segmentIndex: Int)

private fun nearestPointOnPath(
    path: GKPath,
    point: Vector2,
): NearestPathPoint {
    val points = path.points
    if (points.isEmpty()) return NearestPathPoint(point, 0f, 0)
    val segmentCount = if (path.cyclical) points.size else points.size - 1
    var best: NearestPathPoint? = null
    for (i in 0 until segmentCount) {
        val closest = closestPointOnSegment(points[i], points[(i + 1) % points.size], point)
        val distance = (closest - point).length()
        if (best == null || distance < best.distance) best = NearestPathPoint(closest, distance, i)
    }
    return best ?: NearestPathPoint(points.first(), 0f, 0)
}

private fun closestPointOnSegment(
    a: Vector2,
    b: Vector2,
    point: Vector2,
): Vector2 {
    val ab = b - a
    val lengthSquared = ab dot ab
    if (lengthSquared == 0f) return a
    val t = (((point - a) dot ab) / lengthSquared).coerceIn(0f, 1f)
    return a + ab * t
}
