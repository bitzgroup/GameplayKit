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
        val (center, obstacleRadius) = obstacleSphere(agent, obstacle)
        push += pushAway(futurePosition, center, agent.radius + obstacleRadius)
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

// Every obstacle as a sphere in the agent's own coordinates (a GKAgent2D's (x, y) is (x, y, 0) here).
// Apple keeps all agents in one 3D space where a 2D (x, y) is (x, 0, y), so a 2D obstacle at (x, y)
// is at (x, 0, y) to a GKAgent3D, and a sphere's (x, y, z) is (x, z) to a GKAgent2D (verified on
// iOS/tvOS simulators). A polygon is approximated by the smallest circle centered on its centroid
// that encloses every vertex.
private fun obstacleSphere(
    agent: GKAgent,
    obstacle: GKObstacle,
): Pair<Vector3, Float> {
    val is3D = agent is GKAgent3D
    val planar = { point: Vector2 -> if (is3D) Vector3(point.x, 0f, point.y) else Vector3(point.x, point.y, 0f) }
    return when (obstacle) {
        is GKSphereObstacle -> {
            val p = obstacle.position
            (if (is3D) p else Vector3(p.x, p.z, 0f)) to obstacle.radius
        }
        is GKCircleObstacle -> planar(obstacle.position) to obstacle.radius
        is GKPolygonObstacle -> {
            val centroid = polygonCentroid(obstacle.vertices)
            planar(centroid) to (obstacle.vertices.maxOfOrNull { (it - centroid).length() } ?: 0f)
        }
        else -> zero3() to 0f
    }
}

private fun polygonCentroid(vertices: List<Vector2>): Vector2 {
    if (vertices.isEmpty()) return Vector2(0f, 0f)
    var sum = Vector2(0f, 0f)
    vertices.forEach { sum += it }
    return sum * (1f / vertices.size)
}
