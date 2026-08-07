package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKGraphTest {
    @Test
    fun `add and remove update the nodes registry`() {
        val graph = GKGraph()
        val a = GKGraphNode()
        val b = GKGraphNode()

        graph.add(listOf(a, b))
        assertEquals(listOf(a, b), graph.nodes)

        graph.remove(listOf(a))
        assertEquals(listOf(b), graph.nodes)
    }

    @Test
    fun `connectToLowestCostNode links to the cheapest other node in the graph`() {
        val graph = GKGraph()
        val near = GKGraphNode2D(Vector2(1f, 0f))
        val far = GKGraphNode2D(Vector2(10f, 0f))
        val newNode = GKGraphNode2D(Vector2(0f, 0f))
        graph.add(listOf(near, far, newNode))

        graph.connectToLowestCostNode(newNode, bidirectional = true)

        assertEquals(listOf(near), newNode.connectedNodes)
        assertEquals(listOf(newNode), near.connectedNodes)
    }

    // Reproduces GameplayKit's own documented findPath(from:to:) example.
    @Test
    fun `findPath matches GameplayKit's documented example`() {
        val a = GKGraphNode2D(Vector2(0f, 5f))
        val b = GKGraphNode2D(Vector2(3f, 0f))
        val c = GKGraphNode2D(Vector2(2f, 6f))
        val d = GKGraphNode2D(Vector2(4f, 6f))
        val e = GKGraphNode2D(Vector2(2f, 5f))
        val f = GKGraphNode2D(Vector2(2f, 2f))

        a.addConnections(listOf(b, c), bidirectional = false)
        b.addConnections(listOf(e, f), bidirectional = false)
        c.addConnections(listOf(d), bidirectional = false)
        d.addConnections(listOf(e, f), bidirectional = false)

        val graph = GKGraph(listOf(a, b, c, d, e, f))

        assertEquals(listOf(a, c, d, e), graph.findPath(a, e))
        assertEquals(listOf(a, b, f), graph.findPath(a, f))
    }

    @Test
    fun `findPath returns an empty list when no path exists`() {
        val a = GKGraphNode()
        val b = GKGraphNode()
        val graph = GKGraph(listOf(a, b))

        assertTrue(graph.findPath(a, b).isEmpty())
    }
}
