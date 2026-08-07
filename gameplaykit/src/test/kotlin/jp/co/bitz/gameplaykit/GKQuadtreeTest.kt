package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GKQuadtreeTest {
    private fun newTree(): GKQuadtree<String> =
        GKQuadtree(GKQuad(Vector2(0f, 0f), Vector2(16f, 16f)), minimumCellSize = 1f)

    @Test
    fun `add returns a node whose quad contains the point`() {
        val tree = newTree()

        val node = tree.add("a", Vector2(1.5f, 1.5f))

        assertTrue(node.quad.min.x <= 1.5f && node.quad.max.x >= 1.5f)
        assertTrue(node.quad.min.y <= 1.5f && node.quad.max.y >= 1.5f)
    }

    @Test
    fun `elements at point only finds elements registered at that point`() {
        val tree = newTree()
        tree.add("a", Vector2(1f, 1f))
        tree.add("b", Vector2(15f, 15f))

        assertEquals(listOf("a"), tree.elements(Vector2(1f, 1f)))
        assertEquals(listOf("b"), tree.elements(Vector2(15f, 15f)))
        assertEquals(emptyList(), tree.elements(Vector2(8f, 8f)))
    }

    @Test
    fun `elements at point also finds elements registered under a containing quad`() {
        val tree = newTree()
        tree.add("big", GKQuad(Vector2(0f, 0f), Vector2(9f, 9f)))
        tree.add("small", Vector2(1f, 1f))

        assertEquals(setOf("big", "small"), tree.elements(Vector2(1f, 1f)).toSet())
        // Outside "big"'s quad (max 9,9) and no other element is registered there.
        assertEquals(emptyList(), tree.elements(Vector2(10f, 10f)))
    }

    @Test
    fun `elements in quad returns elements whose region intersects the search quad`() {
        val tree = newTree()
        tree.add("a", Vector2(1f, 1f))
        tree.add("b", Vector2(15f, 15f))

        val leftHalf = GKQuad(Vector2(0f, 0f), Vector2(8f, 16f))
        assertEquals(listOf("a"), tree.elements(leftHalf))

        val everything = GKQuad(Vector2(0f, 0f), Vector2(16f, 16f))
        assertEquals(setOf("a", "b"), tree.elements(everything).toSet())
    }

    @Test
    fun `remove by element removes it from wherever it is stored`() {
        val tree = newTree()
        tree.add("big", GKQuad(Vector2(0f, 0f), Vector2(9f, 9f)))
        tree.add("small", Vector2(1f, 1f))

        assertTrue(tree.remove("small"))
        assertEquals(listOf("big"), tree.elements(Vector2(1f, 1f)))
        assertFalse(tree.remove("small"))
    }

    @Test
    fun `remove by element and node removes only from that node`() {
        val tree = newTree()
        val node = tree.add("a", Vector2(1f, 1f))

        assertFalse(tree.remove("a", tree.add("b", Vector2(15f, 15f))))
        assertTrue(tree.remove("a", node))
        assertFalse(tree.remove("a", node))
        assertEquals(emptyList(), tree.elements(Vector2(1f, 1f)))
    }
}
