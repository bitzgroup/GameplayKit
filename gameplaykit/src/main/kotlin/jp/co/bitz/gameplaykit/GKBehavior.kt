package jp.co.bitz.gameplaykit

import kotlin.time.Duration

// A weighted collection of GKGoal objects that together define an agent's steering behavior,
// mirroring GameplayKit's GKBehavior.
public class GKBehavior {
    private val weightsByGoal: LinkedHashMap<GKGoal, Float> = linkedMapOf()

    public val goalCount: Int
        get() = weightsByGoal.size

    public fun weight(goal: GKGoal): Float = weightsByGoal[goal] ?: 0f

    public fun setWeight(
        weight: Float,
        goal: GKGoal,
    ) {
        weightsByGoal[goal] = weight
    }

    public fun removeGoal(goal: GKGoal) {
        weightsByGoal.remove(goal)
    }

    public fun removeAllGoals() {
        weightsByGoal.clear()
    }

    internal fun totalForce(
        agent: GKAgent,
        deltaTime: Duration,
    ): Vector3 {
        var total = Vector3(0f, 0f, 0f)
        weightsByGoal.forEach { (goal, weight) -> total += goal.force(agent, deltaTime) * weight }
        return total
    }

    public companion object {
        public fun of(
            goal: GKGoal,
            weight: Float,
        ): GKBehavior = GKBehavior().apply { setWeight(weight, goal) }

        public fun of(
            goals: List<GKGoal>,
            weights: List<Float>,
        ): GKBehavior = GKBehavior().apply { goals.forEachIndexed { index, goal -> setWeight(weights[index], goal) } }

        public fun of(goalWeights: Map<GKGoal, Float>): GKBehavior =
            GKBehavior().apply { goalWeights.forEach { (goal, weight) -> setWeight(weight, goal) } }
    }
}
