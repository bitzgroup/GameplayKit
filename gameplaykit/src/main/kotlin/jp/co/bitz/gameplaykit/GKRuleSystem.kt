package jp.co.bitz.gameplaykit

// Maintains a collection of GKRule objects and activates them as appropriate, mirroring
// GameplayKit's GKRuleSystem. Supports classical rule evaluation as well as fuzzy logic via
// per-fact grades (assertFact/retractFact accumulate/diminish a fact's grade in [0, 1];
// getMinimumGrade/getMaximumGrade implement fuzzy AND/OR over a set of facts).
public open class GKRuleSystem {
    public val rules: MutableList<GKRule> = mutableListOf()
    public val state: MutableMap<Any, Any> = mutableMapOf()

    private val agendaList: MutableList<GKRule> = mutableListOf()
    private val executedList: MutableList<GKRule> = mutableListOf()
    private val facts: MutableMap<Any, Float> = mutableMapOf()

    // Rules not yet evaluated in the current cycle.
    public val agenda: List<GKRule>
        get() = agendaList.toList()

    // Rules whose predicate evaluated true (and whose action ran) in the current cycle.
    public val executed: List<GKRule>
        get() = executedList.toList()

    public fun addRule(rule: GKRule) {
        rules.add(rule)
        agendaList.add(rule)
    }

    public fun addRules(rules: List<GKRule>) {
        rules.forEach { addRule(it) }
    }

    public fun removeAllRules() {
        rules.clear()
        agendaList.clear()
        executedList.clear()
    }

    public fun assertFact(
        fact: Any,
        grade: Float = 1f,
    ) {
        val newGrade = ((facts[fact] ?: 0f) + grade).coerceIn(0f, 1f)
        facts[fact] = newGrade
    }

    public fun retractFact(
        fact: Any,
        grade: Float = 1f,
    ) {
        val newGrade = ((facts[fact] ?: 0f) - grade).coerceAtLeast(0f)
        if (newGrade <= 0f) facts.remove(fact) else facts[fact] = newGrade
    }

    public fun getGrade(fact: Any): Float = facts[fact] ?: 0f

    // Fuzzy OR: how true is "any of these facts holds".
    public fun getMaximumGrade(facts: List<Any>): Float = facts.maxOfOrNull { getGrade(it) } ?: 0f

    // Fuzzy AND: how true is "all of these facts hold".
    public fun getMinimumGrade(facts: List<Any>): Float = facts.minOfOrNull { getGrade(it) } ?: 0f

    // Evaluates every rule currently in the agenda, in ascending order of salience (ties broken by
    // the order rules were added). Rules whose predicate evaluates true have their action run and
    // move from the agenda to executed.
    public fun evaluate() {
        agendaList.sortedBy { it.salience }.forEach { rule ->
            if (rule.evaluatePredicate(this)) {
                rule.performAction(this)
                agendaList.remove(rule)
                executedList.add(rule)
            }
        }
    }

    // Moves every executed rule back onto the agenda, ready for another evaluate() cycle.
    public fun reset() {
        agendaList.addAll(executedList)
        executedList.clear()
    }
}
