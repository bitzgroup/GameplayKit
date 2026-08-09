package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GKRuleTest {
    @Test
    fun `default rule has zero salience and never fires`() {
        val rule = GKRule()

        assertEquals(0f, rule.salience)
        assertFalse(rule.evaluatePredicate(GKRuleSystem()))
    }

    @Test
    fun `subclasses can override evaluatePredicate and performAction directly`() {
        var actionRan = false
        val rule =
            object : GKRule() {
                override fun evaluatePredicate(system: GKRuleSystem): Boolean = true

                override fun performAction(system: GKRuleSystem) {
                    actionRan = true
                }
            }

        assertEquals(true, rule.evaluatePredicate(GKRuleSystem()))
        rule.performAction(GKRuleSystem())
        assertEquals(true, actionRan)
    }

    @Test
    fun `fromPredicate wires the given lambdas to evaluatePredicate and performAction`() {
        var actionRan = false
        val rule = GKRule.fromPredicate({ true }, { actionRan = true })

        assertEquals(true, rule.evaluatePredicate(GKRuleSystem()))
        rule.performAction(GKRuleSystem())
        assertEquals(true, actionRan)
    }

    @Test
    fun `toAssertFact asserts the fact into the system when the predicate is true`() {
        val system = GKRuleSystem()
        val rule = GKRule.toAssertFact("raining", grade = 0.5f) { true }

        rule.performAction(system)

        assertEquals(0.5f, system.getGrade("raining"))
    }

    @Test
    fun `toRetractFact retracts the fact from the system when the predicate is true`() {
        val system = GKRuleSystem()
        system.assertFact("raining", 0.8f)
        val rule = GKRule.toRetractFact("raining", grade = 0.3f) { true }

        rule.performAction(system)

        assertTrue(abs(system.getGrade("raining") - 0.5f) < 0.0001f)
    }
}
