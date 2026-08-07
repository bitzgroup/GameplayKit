package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKGraphNodeTest {
    @Test
    fun `addConnections one-way only connects the receiver to the given nodes`() {
        val a = GKGraphNode()
        val b = GKGraphNode()

        a.addConnections(listOf(b), bidirectional = false)

        assertEquals(listOf(b), a.connectedNodes)
        assertEquals(emptyList(), b.connectedNodes)
    }

    @Test
    fun `addConnections bidirectional connects both nodes to each other`() {
        val a = GKGraphNode()
        val b = GKGraphNode()

        a.addConnections(listOf(b), bidirectional = true)

        assertEquals(listOf(b), a.connectedNodes)
        assertEquals(listOf(a), b.connectedNodes)
    }

    @Test
    fun `removeConnections bidirectional disconnects both nodes`() {
        val a = GKGraphNode()
        val b = GKGraphNode()
        a.addConnections(listOf(b), bidirectional = true)

        a.removeConnections(listOf(b), bidirectional = true)

        assertEquals(emptyList(), a.connectedNodes)
        assertEquals(emptyList(), b.connectedNodes)
    }

    @Test
    fun `default cost and estimatedCost are a uniform unit step`() {
        val a = GKGraphNode()
        val b = GKGraphNode()

        assertEquals(1f, a.cost(b))
        assertEquals(0f, a.estimatedCost(b))
    }

    @Test
    fun `pathFrom and pathTo find a path across a chain of unit-cost connections`() {
        val a = GKGraphNode()
        val b = GKGraphNode()
        val c = GKGraphNode()
        a.addConnections(listOf(b), bidirectional = true)
        b.addConnections(listOf(c), bidirectional = true)

        assertEquals(listOf(a, b, c), a.pathTo(c))
        assertEquals(listOf(a, b, c), c.pathFrom(a))
    }

    @Test
    fun `pathTo returns an empty list when there is no connection`() {
        val a = GKGraphNode()
        val b = GKGraphNode()

        assertTrue(a.pathTo(b).isEmpty())
    }
}
