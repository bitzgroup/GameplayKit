package jp.co.bitz.gameplaykit

// Collision-avoidance steering formulas, see Steering.kt for the general design note shared across
// all Steering*.kt files.

internal fun avoidAgents(
    agent: GKAgent,
    others: List<GKAgent>,
    maxPredictionTime: Double,
): Vector3 {
    val dt = maxPredictionTime.toFloat()
    val futurePosition = agent.position3 + agent.velocity3 * dt
    var push = zero3()
    others.forEach { other ->
        if (other !== agent) {
            val otherFuture = other.position3 + other.velocity3 * dt
            push += pushAway(futurePosition, otherFuture, agent.radius + other.radius)
        }
    }
    return steerToward(agent, push)
}

internal fun avoidObstacles(
    agent: GKAgent,
    obstacles: List<GKObstacle>,
    maxPredictionTime: Double,
): Vector3 {
    val dt = maxPredictionTime.toFloat()
    val futurePosition = agent.position3 + agent.velocity3 * dt
    var push = zero3()
    obstacles.forEach { obstacle ->
        val (center, obstacleRadius) = obstacleCircle(obstacle)
        val centerPosition = Vector3(center.x, center.y, futurePosition.z)
        push += pushAway(futurePosition, centerPosition, agent.radius + obstacleRadius)
    }
    return steerToward(agent, push)
}

// Returns a vector pointing `from` away from `toward`, scaled by how deeply `from` is within
// `minDistance` of `toward` (zero if `from` is already outside that range).
private fun pushAway(
    from: Vector3,
    toward: Vector3,
    minDistance: Float,
): Vector3 {
    val offset = from - toward
    val distance = offset.length()
    if (distance >= minDistance) return zero3()
    val direction = if (distance > 0f) offset.normalized() else Vector3(1f, 0f, 0f)
    return direction * (minDistance - distance)
}

// Approximates every obstacle as a bounding circle: exact for GKCircleObstacle, and the smallest
// circle (centered on the centroid) enclosing every vertex for GKPolygonObstacle.
private fun obstacleCircle(obstacle: GKObstacle): Pair<Vector2, Float> =
    when (obstacle) {
        is GKCircleObstacle -> obstacle.position to obstacle.radius
        is GKPolygonObstacle -> {
            val centroid = polygonCentroid(obstacle.vertices)
            centroid to (obstacle.vertices.maxOfOrNull { (it - centroid).length() } ?: 0f)
        }
        else -> Vector2(0f, 0f) to 0f
    }

private fun polygonCentroid(vertices: List<Vector2>): Vector2 {
    if (vertices.isEmpty()) return Vector2(0f, 0f)
    var sum = Vector2(0f, 0f)
    vertices.forEach { sum += it }
    return sum * (1f / vertices.size)
}
