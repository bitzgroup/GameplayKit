package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GKDecisionTreeTest {
    // Reproduces the "can I cast any cards" example from Apple's own GKDecisionTree usage
    // discussion (Apple Developer Forums thread 97710): a root question branches on a boolean
    // answer to either a leaf or a second question, which itself branches to leaves.
    @Test
    fun `findAction walks value branches down to the matching leaf`() {
        val tree = GKDecisionTree(attribute = "canCastAny")
        val oneSlotLeft = tree.rootNode.createBranch(value = true, attribute = "oneSlotLeft")
        tree.rootNode.createBranch(value = false, attribute = "moveNextTurn")
        oneSlotLeft.createBranch(value = true, attribute = "printYes")
        oneSlotLeft.createBranch(value = false, attribute = "printNo")

        assertEquals(
            "printYes",
            tree.findAction(mapOf("canCastAny" to true, "oneSlotLeft" to true)),
        )
        assertEquals(
            "printNo",
            tree.findAction(mapOf("canCastAny" to true, "oneSlotLeft" to false)),
        )
        assertEquals(
            "moveNextTurn",
            tree.findAction(mapOf("canCastAny" to false, "oneSlotLeft" to true)),
        )
    }

    @Test
    fun `findAction returns the root's attribute when the tree has no branches`() {
        val tree = GKDecisionTree(attribute = "onlyAction")

        assertEquals("onlyAction", tree.findAction(emptyMap()))
    }

    @Test
    fun `findAction returns null when no branch matches the given answer`() {
        val tree = GKDecisionTree(attribute = "question")
        tree.rootNode.createBranch(value = 1, attribute = "leaf")

        assertNull(tree.findAction(mapOf("question" to 2)))
    }

    @Test
    fun `findAction follows a predicate branch whose predicate matches the answer`() {
        val tree = GKDecisionTree(attribute = "score")
        tree.rootNode.createBranch(predicate = { (it as? Int ?: 0) >= 50 }, attribute = "pass")
        tree.rootNode.createBranch(predicate = { (it as? Int ?: 0) < 50 }, attribute = "fail")

        assertEquals("pass", tree.findAction(mapOf("score" to 80)))
        assertEquals("fail", tree.findAction(mapOf("score" to 20)))
    }

    @Test
    fun `findAction resolves weighted branches using randomSource and only ever returns a configured leaf`() {
        val tree = GKDecisionTree(attribute = "coinFlip")
        tree.rootNode.createBranch(weight = 1, attribute = "heads")
        tree.rootNode.createBranch(weight = 1, attribute = "tails")
        tree.randomSource = GKLinearCongruentialRandomSource(seed = 7L)

        val results = (1..200).map { tree.findAction(emptyMap()) }.toSet()

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it == "heads" || it == "tails" })
    }

    // Classic ID3 training set: the action is "yes" only when both attributes are true (logical
    // AND). ID3 always fits its own training data exactly (each leaf is grown until pure), so
    // findAction must reproduce every training row's action.
    @Test
    fun `the examples-actions-attributes constructor builds a tree that reproduces its training data`() {
        val attributes: List<Any> = listOf("a", "b")
        val examples: List<List<Any>> =
            listOf(
                listOf(true, true),
                listOf(true, false),
                listOf(false, true),
                listOf(false, false),
            )
        val actions: List<Any> = listOf("yes", "no", "no", "no")
        val tree = GKDecisionTree(examples = examples, actions = actions, attributes = attributes)

        examples.forEachIndexed { i, example ->
            val answers: Map<Any, Any> = attributes.zip(example).toMap()
            assertEquals(actions[i], tree.findAction(answers))
        }
    }

    @Test
    fun `prettyPrint renders question nodes and leaves`() {
        val tree = GKDecisionTree(attribute = "question")
        tree.rootNode.createBranch(value = 1, attribute = "leaf")

        val text = tree.prettyPrint()

        assertTrue(text.contains("? question"))
        assertTrue(text.contains("= 1"))
        assertTrue(text.contains("- leaf"))
    }
}
