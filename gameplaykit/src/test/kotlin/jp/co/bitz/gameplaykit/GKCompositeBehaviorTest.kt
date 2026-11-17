package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

/** Mirrors tic-tac-toe's `GKCompositeBehaviorParityTests` (Apple's behavior, recorded on iOS/tvOS simulators). */
class GKCompositeBehaviorTest {
    private val target = GKAgent2D().apply { position = Vector2(100f, 50f) }
    private val seek = GKGoal.toSeekAgent(target)

    private fun endPosition(behavior: GKBehavior): Vector2 {
        val agent =
            GKAgent2D().apply {
                // Limits that never clip this port's seek force (Apple's probe uses its own; the
                // steering forces themselves aren't bit-identical, only how weights combine them).
                maxSpeed = 1_000f
                maxAcceleration = 1_000_000_000f
                mass = 1f
                this.behavior = behavior
            }
        repeat(3) { agent.update((1.0 / 30.0).seconds) }
        return agent.position
    }

    @Test
    fun `a member behavior's weight multiplies its goals' weights`() {
        val inner = GKBehavior.of(seek, 2f)
        val composite = GKCompositeBehavior(listOf(inner), listOf(3f))
        assertEquals(endPosition(GKBehavior.of(seek, 6f)), endPosition(composite))
        assertNotEquals(endPosition(GKBehavior.of(seek, 2f)), endPosition(composite))
        composite.setWeight(0.5f, inner)
        assertEquals(endPosition(GKBehavior.of(seek, 1f)), endPosition(composite))
    }

    @Test
    fun `member behaviors add up and nested composites multiply`() {
        val two = GKCompositeBehavior(listOf(GKBehavior.of(seek, 1f), GKBehavior.of(seek, 1f)), listOf(1f, 1f))
        assertEquals(endPosition(GKBehavior.of(seek, 2f)), endPosition(two))
        val nested =
            GKCompositeBehavior(listOf(GKCompositeBehavior(listOf(GKBehavior.of(seek, 2f)), listOf(3f))), listOf(1f))
        assertEquals(endPosition(GKBehavior.of(seek, 6f)), endPosition(nested))
    }

    @Test
    fun bookkeeping() {
        val inner = GKBehavior.of(seek, 2f)
        val composite = GKCompositeBehavior(listOf(inner), listOf(3f))
        assertEquals(1, composite.behaviorCount)
        assertEquals(0, composite.goalCount)
        assertEquals(3f, composite.weight(inner))
        assertEquals(0f, composite.weight(GKBehavior()))
        assertSame(inner, composite[0])
        assertEquals(1f, GKCompositeBehavior(listOf(inner)).weight(inner))

        val other = GKBehavior.of(seek, 1f)
        composite.setWeight(2f, other)
        assertEquals(2, composite.behaviorCount)
        composite.remove(inner)
        assertSame(other, composite[0])
        composite.removeAllBehaviors()
        assertEquals(0, composite.behaviorCount)
        assertFailsWith<IllegalArgumentException> { GKCompositeBehavior(listOf(inner), listOf(1f, 2f)) }
    }
}
