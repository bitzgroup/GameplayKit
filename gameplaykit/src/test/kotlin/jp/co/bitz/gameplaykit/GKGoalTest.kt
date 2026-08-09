package jp.co.bitz.gameplaykit

import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private fun distance(
    a: Vector2,
    b: Vector2,
): Float = (a - b).length()

class GKGoalTest {
    @Test
    fun `toSeekAgent moves the agent closer to the target`() {
        val target = GKAgent2D().apply { position = Vector2(50f, 0f) }
        val agent =
            GKAgent2D().apply {
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toSeekAgent(target), 1f)
            }
        val initialDistance = distance(agent.position, target.position)

        repeat(30) { agent.update(0.1.seconds) }

        assertTrue(distance(agent.position, target.position) < initialDistance)
    }

    @Test
    fun `toFleeAgent moves the agent farther from the target`() {
        val target = GKAgent2D().apply { position = Vector2(1f, 0f) }
        val agent =
            GKAgent2D().apply {
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toFleeAgent(target), 1f)
            }
        val initialDistance = distance(agent.position, target.position)

        repeat(30) { agent.update(0.1.seconds) }

        assertTrue(distance(agent.position, target.position) > initialDistance)
    }

    @Test
    fun `toAvoidAgents deflects the agent off a collision course`() {
        val obstacleAgent =
            GKAgent2D().apply {
                position = Vector2(20f, 0.3f)
                radius = 2f
            }

        fun coastingAgent() =
            GKAgent2D().apply {
                velocity = Vector2(5f, 0f)
                maxSpeed = 5f
                maxAcceleration = 20f
                radius = 1f
            }
        val straightAgent = coastingAgent()
        val avoidingAgent =
            coastingAgent().apply { behavior = GKBehavior.of(GKGoal.toAvoidAgents(listOf(obstacleAgent), 2.0), 1f) }

        repeat(40) {
            straightAgent.update(0.1.seconds)
            avoidingAgent.update(0.1.seconds)
        }

        assertTrue(abs(avoidingAgent.position.y) > abs(straightAgent.position.y))
    }

    @Test
    fun `toAvoidObstacles deflects the agent around an obstacle in its path`() {
        val obstacle = GKCircleObstacle(radius = 2f, position = Vector2(20f, 0.3f))

        fun coastingAgent() =
            GKAgent2D().apply {
                velocity = Vector2(5f, 0f)
                maxSpeed = 5f
                maxAcceleration = 20f
                radius = 1f
            }
        val straightAgent = coastingAgent()
        val avoidingAgent =
            coastingAgent().apply { behavior = GKBehavior.of(GKGoal.toAvoidObstacles(listOf(obstacle), 2.0), 1f) }

        repeat(40) {
            straightAgent.update(0.1.seconds)
            avoidingAgent.update(0.1.seconds)
        }

        assertTrue(abs(avoidingAgent.position.y) > abs(straightAgent.position.y))
    }

    @Test
    fun `toSeparateFrom pushes close agents apart`() {
        val neighbor = GKAgent2D().apply { position = Vector2(0.5f, 0f) }
        val agent =
            GKAgent2D().apply {
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toSeparateFrom(listOf(neighbor), 10f, PI.toFloat()), 1f)
            }
        val initialDistance = distance(agent.position, neighbor.position)

        repeat(20) { agent.update(0.1.seconds) }

        assertTrue(distance(agent.position, neighbor.position) > initialDistance)
    }

    @Test
    fun `toAlignWith turns the agent's velocity toward its neighbor's heading`() {
        val neighbor = GKAgent2D().apply { velocity = Vector2(5f, 0f) }
        val agent =
            GKAgent2D().apply {
                velocity = Vector2(0f, 5f)
                maxSpeed = 5f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toAlignWith(listOf(neighbor), 10f, PI.toFloat()), 1f)
            }

        repeat(10) { agent.update(0.1.seconds) }

        assertTrue(agent.velocity.x > 0f)
    }

    @Test
    fun `toCohereWith moves the agent toward its neighbor's position`() {
        val neighbor = GKAgent2D().apply { position = Vector2(50f, 0f) }
        val agent =
            GKAgent2D().apply {
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toCohereWith(listOf(neighbor), 100f, PI.toFloat()), 1f)
            }

        repeat(20) { agent.update(0.1.seconds) }

        assertTrue(agent.position.x > 0f)
    }

    @Test
    fun `toReachTargetSpeed moves speed toward the target speed`() {
        val agent =
            GKAgent2D().apply {
                velocity = Vector2(5f, 0f)
                maxSpeed = 100f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toReachTargetSpeed(2f), 1f)
            }

        repeat(20) { agent.update(0.1.seconds) }

        assertTrue(agent.velocity.length() < 5f)
        assertTrue(abs(agent.velocity.length() - 2f) < 0.5f)
    }

    @Test
    fun `toInterceptAgent leads a moving target more effectively than a plain seek`() {
        fun movingTarget() =
            GKAgent2D().apply {
                position = Vector2(50f, 0f)
                velocity = Vector2(0f, 20f)
                maxSpeed = 20f
            }
        val interceptTarget = movingTarget()
        val seekTarget = movingTarget()
        val interceptingAgent =
            GKAgent2D().apply {
                maxSpeed = 30f
                maxAcceleration = 100f
                behavior = GKBehavior.of(GKGoal.toInterceptAgent(interceptTarget, 1.0), 1f)
            }
        val seekingAgent =
            GKAgent2D().apply {
                maxSpeed = 30f
                maxAcceleration = 100f
                behavior = GKBehavior.of(GKGoal.toSeekAgent(seekTarget), 1f)
            }

        repeat(20) {
            interceptTarget.update(0.1.seconds)
            seekTarget.update(0.1.seconds)
            interceptingAgent.update(0.1.seconds)
            seekingAgent.update(0.1.seconds)
        }

        val interceptDistance = distance(interceptingAgent.position, interceptTarget.position)
        val seekDistance = distance(seekingAgent.position, seekTarget.position)
        assertTrue(interceptDistance < seekDistance)
    }

    @Test
    fun `toFollowPath advances the agent along the path in the requested direction`() {
        val path = GKPath(listOf(Vector2(0f, 0f), Vector2(50f, 0f), Vector2(100f, 0f)), radius = 2f)
        val agent =
            GKAgent2D().apply {
                position = Vector2(0f, 5f)
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toFollowPath(path, 1.0, forward = true), 1f)
            }

        repeat(60) { agent.update(0.1.seconds) }

        assertTrue(agent.position.x > 0f)
        assertTrue(abs(agent.position.y) < 5f)
    }

    @Test
    fun `toStayOnPath pulls the agent back once it strays outside the path's radius`() {
        val path = GKPath(listOf(Vector2(-100f, 0f), Vector2(100f, 0f)), radius = 2f)
        val agent =
            GKAgent2D().apply {
                position = Vector2(0f, 10f)
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toStayOnPath(path, 0.5), 1f)
            }

        repeat(30) { agent.update(0.1.seconds) }

        assertTrue(agent.position.y < 10f)
    }

    @Test
    fun `toStayOnPath leaves the agent alone while already within the radius`() {
        val path = GKPath(listOf(Vector2(-100f, 0f), Vector2(100f, 0f)), radius = 5f)
        val agent =
            GKAgent2D().apply {
                position = Vector2(0f, 1f)
                maxSpeed = 10f
                maxAcceleration = 50f
                behavior = GKBehavior.of(GKGoal.toStayOnPath(path, 0.5), 1f)
            }

        repeat(10) { agent.update(0.1.seconds) }

        assertEquals(Vector2(0f, 1f), agent.position)
    }

    @Test
    fun `toWander produces continuous, non-degenerate movement`() {
        val agent =
            GKAgent2D().apply {
                maxSpeed = 5f
                maxAcceleration = 20f
                behavior = GKBehavior.of(GKGoal.toWander(5f), 1f)
            }

        repeat(30) { agent.update(0.1.seconds) }

        assertTrue(agent.position.length() > 0f)
    }
}
