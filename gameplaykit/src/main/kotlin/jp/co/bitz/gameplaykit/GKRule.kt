package jp.co.bitz.gameplaykit

/**
 * A single element, comprising a predicate and an action, that represents a discrete rule in a
 * [GKRuleSystem], mirroring GameplayKit's `GKRule`. Open for subclassing (override
 * [evaluatePredicate]/[performAction] directly) the same way [GKState] is; the companion
 * factories cover the common case with Kotlin lambdas instead — this library has no `NSPredicate`
 * equivalent, so [fromPredicate] takes a plain `(GKRuleSystem) -> Boolean` rather than wrapping
 * GameplayKit's `NSPredicate`-based `GKNSPredicateRule`, which isn't implemented here.
 */
public open class GKRule {
    /** How this rule is ordered relative to others when [GKRuleSystem.evaluate] runs the agenda; lower runs first. */
    public var salience: Float = 0f

    /** Whether this rule should fire this cycle. `false` by default; override in a subclass or use [fromPredicate]. */
    public open fun evaluatePredicate(system: GKRuleSystem): Boolean = false

    /** Runs this rule's effect on [system]. No-op by default; override in a subclass or use [fromPredicate]. */
    public open fun performAction(system: GKRuleSystem) {}

    public companion object {
        /** Creates a [GKRule] whose [evaluatePredicate]/[performAction] delegate to [predicate]/[action]. */
        public fun fromPredicate(
            predicate: (GKRuleSystem) -> Boolean,
            action: (GKRuleSystem) -> Unit,
        ): GKRule = LambdaRule(predicate, action)

        /**
         * Convenience wrapping [fromPredicate]'s common "when true, assert a fact" shape, mirroring
         * GameplayKit's `GKRule.fromPredicateAssertingFact(_:fact:grade:)` but built on Kotlin lambdas
         * rather than `NSPredicate`.
         */
        public fun toAssertFact(
            fact: Any,
            grade: Float = 1f,
            predicate: (GKRuleSystem) -> Boolean,
        ): GKRule = fromPredicate(predicate) { it.assertFact(fact, grade) }

        /** As [toAssertFact], but retracting [fact] instead of asserting it. */
        public fun toRetractFact(
            fact: Any,
            grade: Float = 1f,
            predicate: (GKRuleSystem) -> Boolean,
        ): GKRule = fromPredicate(predicate) { it.retractFact(fact, grade) }
    }
}

private class LambdaRule(
    private val predicate: (GKRuleSystem) -> Boolean,
    private val action: (GKRuleSystem) -> Unit,
) : GKRule() {
    override fun evaluatePredicate(system: GKRuleSystem): Boolean = predicate(system)

    override fun performAction(system: GKRuleSystem) {
        action(system)
    }
}
