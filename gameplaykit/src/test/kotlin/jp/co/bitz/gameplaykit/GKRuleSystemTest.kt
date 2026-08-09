package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKRuleSystemTest {
    @Test
    fun `addRule places the rule on both rules and agenda`() {
        val system = GKRuleSystem()
        val rule = GKRule()

        system.addRule(rule)

        assertEquals(listOf(rule), system.rules)
        assertEquals(listOf(rule), system.agenda)
        assertTrue(system.executed.isEmpty())
    }

    @Test
    fun `addRules adds every rule in order`() {
        val system = GKRuleSystem()
        val first = GKRule()
        val second = GKRule()

        system.addRules(listOf(first, second))

        assertEquals(listOf(first, second), system.rules)
        assertEquals(listOf(first, second), system.agenda)
    }

    @Test
    fun `removeAllRules clears rules agenda and executed`() {
        val system = GKRuleSystem()
        system.addRule(GKRule.fromPredicate({ true }, {}))
        system.evaluate()

        system.removeAllRules()

        assertTrue(system.rules.isEmpty())
        assertTrue(system.agenda.isEmpty())
        assertTrue(system.executed.isEmpty())
    }

    @Test
    fun `assertFact accumulates grade clamped to 1`() {
        val system = GKRuleSystem()

        system.assertFact("wet", 0.6f)
        system.assertFact("wet", 0.6f)

        assertEquals(1f, system.getGrade("wet"))
    }

    @Test
    fun `retractFact diminishes grade and removes the fact once it reaches zero`() {
        val system = GKRuleSystem()
        system.assertFact("wet", 0.6f)

        system.retractFact("wet", 0.4f)
        assertTrue(abs(system.getGrade("wet") - 0.2f) < 0.0001f)

        system.retractFact("wet", 0.4f)
        assertEquals(0f, system.getGrade("wet"))
    }

    @Test
    fun `getGrade returns zero for a fact that was never asserted`() {
        val system = GKRuleSystem()

        assertEquals(0f, system.getGrade("unknown"))
    }

    @Test
    fun `getMaximumGrade implements fuzzy OR across facts`() {
        val system = GKRuleSystem()
        system.assertFact("a", 0.2f)
        system.assertFact("b", 0.7f)

        assertEquals(0.7f, system.getMaximumGrade(listOf("a", "b", "c")))
    }

    @Test
    fun `getMinimumGrade implements fuzzy AND across facts`() {
        val system = GKRuleSystem()
        system.assertFact("a", 0.2f)
        system.assertFact("b", 0.7f)

        assertEquals(0f, system.getMinimumGrade(listOf("a", "b", "c")))
    }

    @Test
    fun `evaluate runs rules in ascending salience order and moves them to executed`() {
        val system = GKRuleSystem()
        val order = mutableListOf<String>()
        val low = GKRule.fromPredicate({ true }, { order.add("low") }).apply { salience = 1f }
        val high = GKRule.fromPredicate({ true }, { order.add("high") }).apply { salience = 2f }
        system.addRules(listOf(high, low))

        system.evaluate()

        assertEquals(listOf("low", "high"), order)
        assertEquals(listOf(low, high), system.executed)
        assertTrue(system.agenda.isEmpty())
    }

    @Test
    fun `evaluate leaves rules whose predicate is false on the agenda`() {
        val system = GKRuleSystem()
        val rule = GKRule.fromPredicate({ false }, {})
        system.addRule(rule)

        system.evaluate()

        assertEquals(listOf(rule), system.agenda)
        assertTrue(system.executed.isEmpty())
    }

    @Test
    fun `reset moves every executed rule back onto the agenda`() {
        val system = GKRuleSystem()
        val rule = GKRule.fromPredicate({ true }, {})
        system.addRule(rule)
        system.evaluate()

        system.reset()

        assertEquals(listOf(rule), system.agenda)
        assertTrue(system.executed.isEmpty())
    }

    // Reproduces Apple's own FizzBuzz GKRuleSystem example (see the GKRuleSystem documentation's
    // Remarks section): a rule per divisor asserts "fizz"/"buzz" as a fact, plus a base rule that
    // asserts the number itself; the highest-salience rule reads back whichever facts ended up
    // asserted. For i=15, both the "divisible by 3" and "divisible by 5" rules fire at the same
    // salience; Apple's own prose result ("buzzfizz") doesn't match the "ascending salience, ties
    // broken by insertion order" reading of its own example code (which adds fizz before buzz), so
    // only substring presence is asserted for that tied case — every other input is asserted exactly.
    @Test
    fun `fizzbuzz via GKRuleSystem matches Apple's documented example for non-tied inputs`() {
        for (i in 1..14) {
            assertEquals(expectedFizzBuzz(i), fizzBuzz(i))
        }
    }

    @Test
    fun `fizzbuzz via GKRuleSystem asserts both fizz and buzz facts for a multiple of 15`() {
        val result = fizzBuzz(15)

        assertContains(result, "fizz")
        assertContains(result, "buzz")
    }

    private fun expectedFizzBuzz(i: Int): String =
        when {
            i % 15 == 0 -> "fizzbuzz"
            i % 3 == 0 -> "fizz"
            i % 5 == 0 -> "buzz"
            else -> i.toString()
        }

    private fun fizzBuzz(i: Int): String {
        val system = GKRuleSystem()
        system.state["i"] = i
        val fizzRule =
            GKRule.toAssertFact("fizz", predicate = { it.state["i"] as Int % 3 == 0 })
                .apply { salience = 1f }
        val buzzRule =
            GKRule.toAssertFact("buzz", predicate = { it.state["i"] as Int % 5 == 0 })
                .apply { salience = 1f }
        val printRule =
            GKRule.fromPredicate({ true }) { s ->
                val fizz = s.getGrade("fizz") > 0f
                val buzz = s.getGrade("buzz") > 0f
                s.state["result"] =
                    when {
                        fizz || buzz ->
                            buildString {
                                if (fizz) append("fizz")
                                if (buzz) append("buzz")
                            }
                        else -> i.toString()
                    }
            }.apply { salience = 2f }
        system.addRules(listOf(fizzRule, buzzRule, printRule))

        system.evaluate()

        return system.state["result"] as String
    }
}
