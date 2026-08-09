package jp.co.bitz.gameplaykit

import kotlin.time.Duration

// Influences the movement of one or more GKAgent objects, mirroring GameplayKit's GKGoal. Rather
// than an opaque native "goal type", a goal is simply a steering-force function of (agent,
// deltaTime) — consistent with this library's use of Kotlin lambdas in place of hidden native
// dispatch (see also GKRule). The steering formulas themselves (in Steering.kt) follow the standard
// Reynolds steering-behavior algorithms; GameplayKit's own internal implementation is undocumented,
// so this is contract-conformant, not guaranteed bit-identical.
public class GKGoal private constructor(
    private val steer: (agent: GKAgent, deltaTime: Duration) -> Vector3,
) {
    internal fun force(
        agent: GKAgent,
        deltaTime: Duration,
    ): Vector3 = steer(agent, deltaTime)

    public companion object {
        public fun toSeekAgent(agent: GKAgent): GKGoal = GKGoal { self, _ -> seekPosition(self, agent.position3) }

        public fun toFleeAgent(agent: GKAgent): GKGoal = GKGoal { self, _ -> fleePosition(self, agent.position3) }

        public fun toAvoidAgents(
            agents: List<GKAgent>,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> avoidAgents(self, agents, maxPredictionTime) }

        public fun toAvoidObstacles(
            obstacles: List<GKObstacle>,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> avoidObstacles(self, obstacles, maxPredictionTime) }

        public fun toSeparateFrom(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> separate(self, agents, maxDistance, maxAngle) }

        public fun toAlignWith(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> align(self, agents, maxDistance, maxAngle) }

        public fun toCohereWith(
            agents: List<GKAgent>,
            maxDistance: Float,
            maxAngle: Float,
        ): GKGoal = GKGoal { self, _ -> cohere(self, agents, maxDistance, maxAngle) }

        public fun toReachTargetSpeed(targetSpeed: Float): GKGoal =
            GKGoal { self, _ -> reachTargetSpeed(self, targetSpeed) }

        public fun toInterceptAgent(
            agent: GKAgent,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> interceptAgent(self, agent, maxPredictionTime) }

        public fun toFollowPath(
            path: GKPath,
            maxPredictionTime: Double,
            forward: Boolean,
        ): GKGoal = GKGoal { self, _ -> followPath(self, path, maxPredictionTime, forward) }

        public fun toStayOnPath(
            path: GKPath,
            maxPredictionTime: Double,
        ): GKGoal = GKGoal { self, _ -> stayOnPath(self, path, maxPredictionTime) }

        public fun toWander(speed: Float): GKGoal {
            var wanderAngle = 0f
            return GKGoal { self, _ ->
                wanderAngle = nextWanderAngle(wanderAngle)
                wander(self, speed, wanderAngle)
            }
        }
    }
}
