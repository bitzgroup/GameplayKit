package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GKOctreeTest {
    private fun newTree(): GKOctree<String> =
        GKOctree(GKBox(Vector3(0f, 0f, 0f), Vector3(16f, 16f, 16f)), minimumCellSize = 1f)

    @Test
    fun `add returns a node whose box contains the point`() {
        val tree = newTree()

        val node = tree.add("a", Vector3(1.5f, 1.5f, 1.5f))

        assertTrue(node.box.min.x <= 1.5f && node.box.max.x >= 1.5f)
        assertTrue(node.box.min.y <= 1.5f && node.box.max.y >= 1.5f)
        assertTrue(node.box.min.z <= 1.5f && node.box.max.z >= 1.5f)
    }

    @Test
    fun `elements at point only finds elements registered at that point`() {
        val tree = newTree()
        tree.add("a", Vector3(1f, 1f, 1f))
        tree.add("b", Vector3(15f, 15f, 15f))

        assertEquals(listOf("a"), tree.elements(Vector3(1f, 1f, 1f)))
        assertEquals(listOf("b"), tree.elements(Vector3(15f, 15f, 15f)))
        assertEquals(emptyList(), tree.elements(Vector3(8f, 8f, 8f)))
    }

    @Test
    fun `elements at point also finds elements registered under a containing box`() {
        val tree = newTree()
        tree.add("big", GKBox(Vector3(0f, 0f, 0f), Vector3(9f, 9f, 9f)))
        tree.add("small", Vector3(1f, 1f, 1f))

        assertEquals(setOf("big", "small"), tree.elements(Vector3(1f, 1f, 1f)).toSet())
        // Outside "big"'s box (max 9,9,9) and no other element is registered there.
        assertEquals(emptyList(), tree.elements(Vector3(10f, 10f, 10f)))
    }

    @Test
    fun `elements in box returns elements whose region intersects the search box`() {
        val tree = newTree()
        tree.add("a", Vector3(1f, 1f, 1f))
        tree.add("b", Vector3(15f, 15f, 15f))

        val lowerHalf = GKBox(Vector3(0f, 0f, 0f), Vector3(8f, 16f, 16f))
        assertEquals(listOf("a"), tree.elements(lowerHalf))

        val everything = GKBox(Vector3(0f, 0f, 0f), Vector3(16f, 16f, 16f))
        assertEquals(setOf("a", "b"), tree.elements(everything).toSet())
    }

    @Test
    fun `remove by element removes it from wherever it is stored`() {
        val tree = newTree()
        tree.add("big", GKBox(Vector3(0f, 0f, 0f), Vector3(9f, 9f, 9f)))
        tree.add("small", Vector3(1f, 1f, 1f))

        assertTrue(tree.remove("small"))
        assertEquals(listOf("big"), tree.elements(Vector3(1f, 1f, 1f)))
        assertFalse(tree.remove("small"))
    }

    @Test
    fun `remove by element and node removes only from that node`() {
        val tree = newTree()
        val node = tree.add("a", Vector3(1f, 1f, 1f))

        assertFalse(tree.remove("a", tree.add("b", Vector3(15f, 15f, 15f))))
        assertTrue(tree.remove("a", node))
        assertFalse(tree.remove("a", node))
        assertEquals(emptyList(), tree.elements(Vector3(1f, 1f, 1f)))
    }
}
