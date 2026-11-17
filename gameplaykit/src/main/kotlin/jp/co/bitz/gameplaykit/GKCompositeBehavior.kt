package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * A [GKBehavior] made of other behaviors, each with a weight — mirroring GameplayKit's
 * `GKCompositeBehavior`. Its steering force is the weighted sum of its behaviors' forces, so a
 * member's weight multiplies the weights of that member's goals, and nested composites multiply
 * through (verified on iOS/tvOS simulators against equivalent plain behaviors).
 *
 * Deviation: goals added to the composite itself ([setWeight] with a [GKGoal]) are added to the
 * sum too. On Apple, an agent whose composite behavior has goals of its own crashes on update.
 *
 * @param behaviors The member behaviors, in order.
 * @param weights Each behavior's weight, by index — `1` for every one by default, as Apple's
 *   `init(behaviors:)` gives.
 */
public class GKCompositeBehavior(
    behaviors: List<GKBehavior> = emptyList(),
    weights: List<Float> = List(behaviors.size) { 1f },
) : GKBehavior() {
    private val weightsByBehavior: LinkedHashMap<GKBehavior, Float> = linkedMapOf()

    init {
        require(weights.size == behaviors.size) { "One weight is needed per behavior." }
        behaviors.forEachIndexed { index, behavior -> weightsByBehavior[behavior] = weights[index] }
    }

    /** The number of member behaviors — Apple's `behaviorCount`. */
    public val behaviorCount: Int
        get() = weightsByBehavior.size

    /** The member behavior at [index], in the order they were added — Apple's `subscript(_:)`. */
    public operator fun get(index: Int): GKBehavior = weightsByBehavior.keys.elementAt(index)

    /** [behavior]'s weight, or `0` if it isn't a member — Apple's `weight(for:)`. */
    public fun weight(behavior: GKBehavior): Float = weightsByBehavior[behavior] ?: 0f

    /** Adds [behavior] with [weight], or updates its weight if it's already a member — Apple's `setWeight(_:for:)`. */
    public fun setWeight(
        weight: Float,
        behavior: GKBehavior,
    ) {
        weightsByBehavior[behavior] = weight
    }

    /** Removes [behavior] — Apple's `remove(_:)`. */
    public fun remove(behavior: GKBehavior) {
        weightsByBehavior.remove(behavior)
    }

    /** Removes every member behavior — Apple's `removeAllBehaviors()`. Goals of its own stay. */
    public fun removeAllBehaviors() {
        weightsByBehavior.clear()
    }

    override fun totalForce(
        agent: GKAgent,
        deltaTime: Duration,
    ): Vector3 {
        var total = super.totalForce(agent, deltaTime)
        weightsByBehavior.forEach { (behavior, weight) -> total += behavior.totalForce(agent, deltaTime) * weight }
        return total
    }
}
