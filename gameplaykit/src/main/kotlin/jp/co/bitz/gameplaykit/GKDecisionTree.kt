package jp.co.bitz.gameplaykit

import kotlin.math.log2

// A tree of questions, answers, and actions used to drive AI decision-making, mirroring
// GameplayKit's GKDecisionTree. Build a tree by branching off `rootNode` (see
// GKDecisionNode.createBranch), then query it with findAction; or grow one automatically from a
// set of training examples with the `examples`/`actions`/`attributes` constructor.
//
// Not implemented: GameplayKit's NSCoder/NSURL-based initializers and `export(to:)`. This library
// has no NSCoding/archiving equivalent anywhere else either (see GKRuleSystem's fact model, which
// is likewise plain in-memory state), so tree persistence is left to the host application.
public class GKDecisionTree private constructor(
    public val rootNode: GKDecisionNode,
) {
    // Creates a tree with a single root node asking about `attribute`; branch it out with
    // `rootNode.createBranch(...)`.
    public constructor(attribute: Any) : this(GKDecisionNode(attribute))

    // Creates a tree by inductive learning (the classic ID3 algorithm: at each step, split on the
    // attribute with the highest information gain) from a training data set. `examples[i]` is one
    // training row, positionally matching `attributes`; `actions[i]` is that row's outcome.
    // GameplayKit doesn't document its own ID3 variant's tie-breaking/pruning behavior, so this is
    // contract-conformant (produces a tree consistent with the training data), not bit-identical.
    public constructor(examples: List<List<Any>>, actions: List<Any>, attributes: List<Any>) :
        this(buildTree(examples, actions, attributes))

    // Used to resolve GKDecisionNode.createBranch(weight:attribute:) branches in findAction.
    public var randomSource: GKRandomSource = GKRandomSource.sharedRandom()

    // Walks the tree from the root, following whichever branch matches `answers`, until it
    // reaches a leaf (a node with no branches), and returns that leaf's attribute as the chosen
    // action. Returns null if a question node's branches don't cover the given answer.
    public fun findAction(answers: Map<Any, Any>): Any? {
        var node = rootNode
        while (node.branches.isNotEmpty()) {
            node = selectBranch(node, answers) ?: return null
        }
        return node.attribute
    }

    // Renders the tree as indented text (`?` for question nodes, `-` for leaves/actions). Not
    // part of GameplayKit's API — Apple exposes no tree introspection at all — added here since a
    // Kotlin developer has no other way to inspect or debug a tree they've built.
    public fun prettyPrint(): String = StringBuilder().also { appendNode(it, rootNode, 0) }.toString()

    private fun selectBranch(
        node: GKDecisionNode,
        answers: Map<Any, Any>,
    ): GKDecisionNode? {
        val weighted = node.branches.filterIsInstance<GKDecisionNode.Branch.Weight>()
        if (weighted.isNotEmpty()) return pickWeighted(weighted)

        val answer = answers[node.attribute]
        return node.branches.firstNotNullOfOrNull { branch ->
            when (branch) {
                is GKDecisionNode.Branch.Value -> branch.child.takeIf { branch.value == answer }
                is GKDecisionNode.Branch.Predicate -> branch.child.takeIf { branch.predicate(answer) }
                is GKDecisionNode.Branch.Weight -> null
            }
        }
    }

    private fun pickWeighted(branches: List<GKDecisionNode.Branch.Weight>): GKDecisionNode {
        val total = branches.sumOf { it.weight }
        var roll = randomSource.nextInt(total)
        for (branch in branches) {
            if (roll < branch.weight) return branch.child
            roll -= branch.weight
        }
        return branches.last().child
    }

    private fun appendNode(
        builder: StringBuilder,
        node: GKDecisionNode,
        depth: Int,
    ) {
        val indent = "  ".repeat(depth)
        if (node.branches.isEmpty()) {
            builder.appendLine("$indent- ${node.attribute}")
            return
        }
        builder.appendLine("$indent? ${node.attribute}")
        node.branches.forEach { branch ->
            builder.appendLine("$indent  ${branchLabel(branch)}")
            appendNode(builder, branch.child, depth + 2)
        }
    }

    private fun branchLabel(branch: GKDecisionNode.Branch): String =
        when (branch) {
            is GKDecisionNode.Branch.Value -> "= ${branch.value}"
            is GKDecisionNode.Branch.Predicate -> "predicate"
            is GKDecisionNode.Branch.Weight -> "weight ${branch.weight}"
        }

    private companion object {
        private data class Row(val values: Map<Any, Any>, val action: Any)

        private fun buildTree(
            examples: List<List<Any>>,
            actions: List<Any>,
            attributes: List<Any>,
        ): GKDecisionNode {
            val rows = examples.indices.map { i -> Row(attributes.zip(examples[i]).toMap(), actions[i]) }
            return buildNode(rows, attributes)
        }

        private fun buildNode(
            rows: List<Row>,
            attributes: List<Any>,
        ): GKDecisionNode {
            val distinctActions = rows.map { it.action }.distinct()
            val majorityAction =
                rows.groupingBy { it.action }.eachCount().entries.maxByOrNull { it.value }?.key ?: rows.first().action

            return when {
                distinctActions.size == 1 -> GKDecisionNode(distinctActions.first())
                attributes.isEmpty() -> GKDecisionNode(majorityAction)
                else -> buildQuestionNode(rows, attributes)
            }
        }

        private fun buildQuestionNode(
            rows: List<Row>,
            attributes: List<Any>,
        ): GKDecisionNode {
            val bestAttribute = attributes.maxByOrNull { informationGain(rows, it) } ?: attributes.first()
            val node = GKDecisionNode(bestAttribute)
            val remainingAttributes = attributes - bestAttribute
            rows.groupBy { it.values.getValue(bestAttribute) }.forEach { (value, subset) ->
                val child = buildNode(subset, remainingAttributes)
                node.branches.add(GKDecisionNode.Branch.Value(value, child))
            }
            return node
        }

        private fun entropy(rows: List<Row>): Double {
            val total = rows.size.toDouble()
            return rows.groupingBy { it.action }.eachCount().values.sumOf { count ->
                val p = count / total
                -p * log2(p)
            }
        }

        private fun informationGain(
            rows: List<Row>,
            attribute: Any,
        ): Double {
            val total = rows.size.toDouble()
            val remainder =
                rows.groupBy { it.values.getValue(attribute) }.values.sumOf { subset ->
                    (subset.size / total) * entropy(subset)
                }
            return entropy(rows) - remainder
        }
    }
}
