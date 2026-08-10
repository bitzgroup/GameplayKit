package jp.co.bitz.gameplaykit

/**
 * A single node in a [GKDecisionTree], mirroring GameplayKit's `GKDecisionNode`. A node with no
 * branches is a leaf, and [attribute] is the action [GKDecisionTree.findAction] returns for it. A
 * node with branches instead uses [attribute] as a question: [GKDecisionTree.findAction] looks
 * that key up in the answers map it's given, and descends into whichever branch matches the
 * answer.
 *
 * Deviation from GameplayKit: [attribute] is a public, readable property here. Apple's real
 * `GKDecisionNode` keeps it private — the only public surface is building ([createBranch]) and
 * querying ([GKDecisionTree.findAction]) — but a Kotlin developer has no other way to introspect
 * or debug a tree they've built, so it's exposed here (also used by [GKDecisionTree.prettyPrint]).
 * There is likewise no public constructor: nodes only come from [GKDecisionTree]'s root or from
 * [createBranch], matching GameplayKit, which has no public `GKDecisionNode` initializer either.
 */
public class GKDecisionNode internal constructor(
    public var attribute: Any,
) {
    internal val branches: MutableList<Branch> = mutableListOf()

    /** Creates a child reached when the answer to this node's [attribute] equals [value]. */
    public fun createBranch(
        value: Any,
        attribute: Any,
    ): GKDecisionNode = addBranch(Branch.Value(value, GKDecisionNode(attribute)))

    /**
     * Creates a child reached when [predicate] returns true for the answer to this node's
     * [attribute]. Takes a Kotlin lambda rather than GameplayKit's `NSPredicate`, per this
     * library's established design principle (see [GKRule]).
     */
    public fun createBranch(
        predicate: (Any?) -> Boolean,
        attribute: Any,
    ): GKDecisionNode = addBranch(Branch.Predicate(predicate, GKDecisionNode(attribute)))

    /**
     * Creates a child reached by a random choice, biased by [weight] relative to this node's
     * other weighted branches, using the owning [GKDecisionTree]'s [GKDecisionTree.randomSource].
     */
    public fun createBranch(
        weight: Int,
        attribute: Any,
    ): GKDecisionNode = addBranch(Branch.Weight(weight, GKDecisionNode(attribute)))

    private fun addBranch(branch: Branch): GKDecisionNode {
        branches.add(branch)
        return branch.child
    }

    internal sealed class Branch(val child: GKDecisionNode) {
        class Value(val value: Any, child: GKDecisionNode) : Branch(child)

        class Predicate(val predicate: (Any?) -> Boolean, child: GKDecisionNode) : Branch(child)

        class Weight(val weight: Int, child: GKDecisionNode) : Branch(child)
    }
}
