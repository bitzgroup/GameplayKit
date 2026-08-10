package jp.co.bitz.gameplaykit

import kotlin.time.Duration

/**
 * Influences the movement of one or more [GKAgent] objects, mirroring GameplayKit's `GKGoal`.
 * Rather than an opaque native "goal type", a goal is simply a steering-force function of
 * `(agent, deltaTime)` — consistent with this library's use of Kotlin lambdas in place of hidden
 * native dispatch (see also [GKRule]). The steering formulas themselves (in `Steering.kt`) follow
 * the standard Reynolds steering-behavior algorithms; GameplayKit's own internal implementation is
 * undocumented, so this is contract-conformant, not guaranteed bit-identical. Combine goals into a
 * [GKBehavior] to drive an agent.
 */
public class GKGoal private constructor(
    private val steer: (agent: GKAgent, deltaTime: Duration) -> Vector3,
) {
    internal fun force(
        agent: GKAgent,
        deltaTime: Duration,
    ): Vector3 = steer(agent, deltaTime)

    public companion object {
        /** A goal that steers directly toward [agent]'s current position. */
        public fun toSeekAgent(agent: GKAgent): GKGoal = GKGoal { self, _ -> seekPosition(self, agent.position3) }

        /** A goal that steers directly away from [agent]'s current position. */
        public fun toFleeAgent(agent: GKAgent): GKGoal = GKGoal { self, _ -> fleePosition(self, agent.position3) }

        /** A goal that steers to avoid colliding with [agents], predicting up to [maxPredictionTime] ahead. */
        public fun toAvoidAgents(
            agents: List<GKAgent>,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> avoidAgents(self, agents, maxPredictionTime) }

        /** A goal that steers to avoid colliding with [obstacles], predicting up to [maxPredictionTime] ahead. */
        public fun toAvoidObstacles(
            obstacles: List<GKObstacle>,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> avoidObstacles(self, obstacles, maxPredictionTime) }

        /** A goal that steers away from nearby [agents], keeping at least [maxDistance] within [maxAngle] of view. */
        public fun toSeparateFrom(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> separate(self, agents, maxDistance, maxAngle) }

        /** A goal that steers to match the heading of nearby [agents] within [maxDistance]/[maxAngle]. */
        public fun toAlignWith(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> align(self, agents, maxDistance, maxAngle) }

        /** A goal that steers toward the average position of nearby [agents] within [maxDistance]/[maxAngle]. */
        public fun toCohereWith(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> cohere(self, agents, maxDistance, maxAngle) }

        /** A goal that steers to accelerate/decelerate toward [targetSpeed], without changing heading. */
        public fun toReachTargetSpeed(targetSpeed: Float): GKGoal =
            GKGoal { self, _ -> reachTargetSpeed(self, targetSpeed) }

        /** A goal that steers to intercept [agent]'s predicted future position, up to [maxPredictionTime] ahead. */
        public fun toInterceptAgent(
            agent: GKAgent,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> interceptAgent(self, agent, maxPredictionTime) }

        /**
         * A goal that steers to follow [path], predicting up to [maxPredictionTime] ahead; if
         * [forward] is `false`, follows the path in reverse.
         */
        public fun toFollowPath(
            path: GKPath,
            maxPredictionTime: Double,
            forward: Boolean,
        ): GKGoal = GKGoal { self, _ -> followPath(self, path, maxPredictionTime, forward) }

        /** A goal that steers back toward [path] whenever the agent strays more than its radius from it. */
        public fun toStayOnPath(
            path: GKPath,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> stayOnPath(self, path, maxPredictionTime) }

        /** A goal that steers in a smoothly-varying random direction at [speed]. */
        public fun toWander(speed: Float): GKGoal {
            var wanderAngle = 0f
            return GKGoal { self, _ ->
                wanderAngle = nextWanderAngle(wanderAngle)
                wander(self, speed, wanderAngle)
            }
        }
    }
}
