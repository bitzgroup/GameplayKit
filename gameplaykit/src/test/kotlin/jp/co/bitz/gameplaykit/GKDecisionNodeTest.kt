package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKDecisionNodeTest {
    @Test
    fun `createBranch with a value returns a child carrying the given attribute`() {
        val node = GKDecisionNode("question")

        val child = node.createBranch(value = 1, attribute = "answer")

        assertEquals("answer", child.attribute)
        assertEquals(1, node.branches.size)
    }

    @Test
    fun `createBranch with a predicate returns a child carrying the given attribute`() {
        val node = GKDecisionNode("question")

        val child = node.createBranch(predicate = { it == true }, attribute = "answer")

        assertEquals("answer", child.attribute)
        assertEquals(1, node.branches.size)
    }

    @Test
    fun `createBranch with a weight returns a child carrying the given attribute`() {
        val node = GKDecisionNode("question")

        val child = node.createBranch(weight = 3, attribute = "answer")

        assertEquals("answer", child.attribute)
        assertEquals(1, node.branches.size)
    }

    @Test
    fun `a node with no branches has an empty branches list`() {
        val node = GKDecisionNode("leaf")

        assertTrue(node.branches.isEmpty())
    }

    @Test
    fun `multiple createBranch calls accumulate on the same node`() {
        val node = GKDecisionNode("question")

        node.createBranch(value = 1, attribute = "yes")
        node.createBranch(value = 0, attribute = "no")

        assertEquals(2, node.branches.size)
    }
}
