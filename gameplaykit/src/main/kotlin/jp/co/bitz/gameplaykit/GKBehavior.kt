package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * A weighted collection of [GKGoal] objects that together define an agent's steering behavior,
 * mirroring GameplayKit's `GKBehavior`. Assign to [GKAgent.behavior] to drive an agent.
 */
public class GKBehavior {
    private val weightsByGoal: LinkedHashMap<GKGoal, Float> = linkedMapOf()

    /** The number of goals currently in this behavior. */
    public val goalCount: Int
        get() = weightsByGoal.size

    /** The weight [goal] contributes with, or `0` if [goal] isn't in this behavior. */
    public fun weight(goal: GKGoal): Float = weightsByGoal[goal] ?: 0f

    /** Adds [goal] to this behavior (or updates its weight, if already present) with the given [weight]. */
    public fun setWeight(
        weight: Float,
        goal: GKGoal,
    ) {
        weightsByGoal[goal] = weight
    }

    /** Removes [goal] from this behavior. */
    public fun removeGoal(goal: GKGoal) {
        weightsByGoal.remove(goal)
    }

    /** Removes every goal from this behavior. */
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
        /** Creates a behavior containing just [goal], weighted by [weight]. */
        public fun of(
            goal: GKGoal,
            weight: Float,
        ): GKBehavior = GKBehavior().apply { setWeight(weight, goal) }

        /** Creates a behavior from [goals], each weighted by the [weights] entry at the same index. */
        public fun of(
            goals: List<GKGoal>,
            weights: List<Float>,
        ): GKBehavior = GKBehavior().apply { goals.forEachIndexed { index, goal -> setWeight(weights[index], goal) } }

        /** Creates a behavior from [goalWeights], mapping each goal to its weight. */
        public fun of(goalWeights: Map<GKGoal, Float>): GKBehavior =
            GKBehavior().apply { goalWeights.forEach { (goal, weight) -> setWeight(weight, goal) } }
    }
}
