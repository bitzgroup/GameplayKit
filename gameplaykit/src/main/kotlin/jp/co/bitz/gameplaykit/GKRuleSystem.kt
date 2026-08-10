package jp.co.bitz.gameplaykit

/**
 * Maintains a collection of [GKRule] objects and activates them as appropriate, mirroring
 * GameplayKit's `GKRuleSystem`. Supports classical rule evaluation as well as fuzzy logic via
 * per-fact grades ([assertFact]/[retractFact] accumulate/diminish a fact's grade in `[0, 1]`;
 * [getMinimumGrade]/[getMaximumGrade] implement fuzzy AND/OR over a set of facts).
 */
public open class GKRuleSystem {
    /** Every rule ever added via [addRule]/[addRules], regardless of agenda/executed state. */
    public val rules: MutableList<GKRule> = mutableListOf()

    /** Arbitrary state shared between rules' predicates and actions during evaluation. */
    public val state: MutableMap<Any, Any> = mutableMapOf()

    private val agendaList: MutableList<GKRule> = mutableListOf()
    private val executedList: MutableList<GKRule> = mutableListOf()
    private val facts: MutableMap<Any, Float> = mutableMapOf()

    /** Rules not yet evaluated in the current cycle. */
    public val agenda: List<GKRule>
        get() = agendaList.toList()

    /** Rules whose predicate evaluated true (and whose action ran) in the current cycle. */
    public val executed: List<GKRule>
        get() = executedList.toList()

    /** Adds [rule] to [rules] and puts it on the agenda for the next [evaluate] cycle. */
    public fun addRule(rule: GKRule) {
        rules.add(rule)
        agendaList.add(rule)
    }

    /** Adds every rule in [rules] via [addRule]. */
    public fun addRules(rules: List<GKRule>) {
        rules.forEach { addRule(it) }
    }

    /** Clears [rules], [agenda], and [executed]. Facts asserted via [assertFact] are left untouched. */
    public fun removeAllRules() {
        rules.clear()
        agendaList.clear()
        executedList.clear()
    }

    /** Raises [fact]'s grade by [grade] (default fully true), clamped to `[0, 1]`. */
    public fun assertFact(
        fact: Any,
        grade: Float = 1f,
    ) {
        val newGrade = ((facts[fact] ?: 0f) + grade).coerceIn(0f, 1f)
        facts[fact] = newGrade
    }

    /** Lowers [fact]'s grade by [grade] (default fully true), removing it once it reaches zero. */
    public fun retractFact(
        fact: Any,
        grade: Float = 1f,
    ) {
        val newGrade = ((facts[fact] ?: 0f) - grade).coerceAtLeast(0f)
        if (newGrade <= 0f) facts.remove(fact) else facts[fact] = newGrade
    }

    /** [fact]'s current grade in `[0, 1]`, or `0` if it has never been asserted. */
    public fun getGrade(fact: Any): Float = facts[fact] ?: 0f

    /** Fuzzy OR: how true is "any of [facts] holds", i.e. the maximum of their grades. */
    public fun getMaximumGrade(facts: List<Any>): Float = facts.maxOfOrNull { getGrade(it) } ?: 0f

    /** Fuzzy AND: how true is "all of [facts] hold", i.e. the minimum of their grades. */
    public fun getMinimumGrade(facts: List<Any>): Float = facts.minOfOrNull { getGrade(it) } ?: 0f

    /**
     * Evaluates every rule currently in [agenda], in ascending order of [GKRule.salience] (ties
     * broken by the order rules were added). Rules whose predicate evaluates true have their
     * action run and move from the agenda to [executed].
     */
    public fun evaluate() {
        agendaList.sortedBy { it.salience }.forEach { rule ->
            if (rule.evaluatePredicate(this)) {
                rule.performAction(this)
                agendaList.remove(rule)
                executedList.add(rule)
            }
        }
    }

    /** Moves every rule in [executed] back onto [agenda], ready for another [evaluate] cycle. */
    public fun reset() {
        agendaList.addAll(executedList)
        executedList.clear()
    }
}
