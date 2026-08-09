package jp.co.bitz.gameplaykit

// Reynolds-style steering formulas backing GKGoal's factory methods, split across this file and its
// SteeringAvoidance/SteeringGroup/SteeringPathAndWander siblings (one file per goal family, to keep
// each file's function count manageable). All operate on the agent's shared Vector3 state (see
// GKAgent), which is how a single implementation serves both GKAgent2D and GKAgent3D.

internal fun zero3(): Vector3 = Vector3(0f, 0f, 0f)

internal fun seekPosition(
    agent: GKAgent,
    target: Vector3,
): Vector3 {
    val toTarget = target - agent.position3
    val desired = if (toTarget.length() > 0f) toTarget.normalized() * agent.maxSpeed else zero3()
    return desired - agent.velocity3
}

internal fun fleePosition(
    agent: GKAgent,
    target: Vector3,
): Vector3 {
    val away = agent.position3 - target
    val desired = if (away.length() > 0f) away.normalized() * agent.maxSpeed else zero3()
    return desired - agent.velocity3
}

internal fun reachTargetSpeed(
    agent: GKAgent,
    targetSpeed: Float,
): Vector3 {
    if (agent.velocity3.length() == 0f) return zero3()
    return agent.velocity3.normalized() * targetSpeed - agent.velocity3
}

internal fun interceptAgent(
    agent: GKAgent,
    target: GKAgent,
    maxPredictionTime: Double,
): Vector3 {
    val predicted = target.position3 + target.velocity3 * maxPredictionTime.toFloat()
    return seekPosition(agent, predicted)
}
