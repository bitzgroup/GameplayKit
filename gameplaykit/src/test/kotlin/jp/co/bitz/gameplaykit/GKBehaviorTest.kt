package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKBehaviorTest {
    @Test
    fun `starts with no goals`() {
        assertEquals(0, GKBehavior().goalCount)
    }

    @Test
    fun `setWeight adds a goal, and weight returns 0 for a goal never added`() {
        val behavior = GKBehavior()
        val goal = GKGoal.toWander(1f)
        val neverAdded = GKGoal.toWander(1f)

        behavior.setWeight(0.5f, goal)

        assertEquals(1, behavior.goalCount)
        assertEquals(0.5f, behavior.weight(goal))
        assertEquals(0f, behavior.weight(neverAdded))
    }

    @Test
    fun `removeGoal and removeAllGoals shrink the goal count`() {
        val behavior = GKBehavior()
        val a = GKGoal.toWander(1f)
        val b = GKGoal.toWander(1f)
        behavior.setWeight(1f, a)
        behavior.setWeight(1f, b)

        behavior.removeGoal(a)
        assertEquals(1, behavior.goalCount)

        behavior.removeAllGoals()
        assertEquals(0, behavior.goalCount)
    }

    @Test
    fun `of a single goal and weight`() {
        val goal = GKGoal.toWander(1f)

        val behavior = GKBehavior.of(goal, 2f)

        assertEquals(1, behavior.goalCount)
        assertEquals(2f, behavior.weight(goal))
    }

    @Test
    fun `of a list of goals and matching weights`() {
        val a = GKGoal.toWander(1f)
        val b = GKGoal.toWander(1f)

        val behavior = GKBehavior.of(listOf(a, b), listOf(1f, 2f))

        assertEquals(1f, behavior.weight(a))
        assertEquals(2f, behavior.weight(b))
    }

    @Test
    fun `of a map of goals to weights`() {
        val a = GKGoal.toWander(1f)
        val b = GKGoal.toWander(1f)

        val behavior = GKBehavior.of(mapOf(a to 1f, b to 2f))

        assertEquals(1f, behavior.weight(a))
        assertEquals(2f, behavior.weight(b))
    }
}
