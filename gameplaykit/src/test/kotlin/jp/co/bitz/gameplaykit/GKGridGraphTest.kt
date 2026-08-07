package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GKGridGraphTest {
    @Test
    fun `builds a node at every grid position`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 3, gridHeight = 2, diagonalsAllowed = false)

        assertEquals(6, graph.nodes.size)
        assertNotNull(graph.node(Vector2Int(0, 0)))
        assertNotNull(graph.node(Vector2Int(2, 1)))
        assertNull(graph.node(Vector2Int(3, 0)))
    }

    @Test
    fun `without diagonals an interior node connects only to its 4 orthogonal neighbors`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 3, gridHeight = 3, diagonalsAllowed = false)

        val center = graph.node(Vector2Int(1, 1))!!

        assertEquals(4, center.connectedNodes.size)
        assertTrue(center.connectedNodes.all { it is GKGridGraphNode })
    }

    @Test
    fun `with diagonals an interior node connects to all 8 neighbors`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 3, gridHeight = 3, diagonalsAllowed = true)

        val center = graph.node(Vector2Int(1, 1))!!

        assertEquals(8, center.connectedNodes.size)
    }

    @Test
    fun `a corner node only connects to neighbors that exist within the grid`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 3, gridHeight = 3, diagonalsAllowed = true)

        val corner = graph.node(Vector2Int(0, 0))!!

        assertEquals(3, corner.connectedNodes.size)
    }

    @Test
    fun `adjacent nodes report unit cost whether orthogonal or diagonal`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 3, gridHeight = 3, diagonalsAllowed = true)
        val center = graph.node(Vector2Int(1, 1))!!
        val orthogonalNeighbor = graph.node(Vector2Int(1, 0))!!
        val diagonalNeighbor = graph.node(Vector2Int(0, 0))!!

        assertEquals(1f, center.cost(orthogonalNeighbor))
        assertEquals(1f, center.cost(diagonalNeighbor))
    }

    @Test
    fun `findPath crosses the grid from corner to corner`() {
        val graph = GKGridGraph(Vector2Int(0, 0), gridWidth = 4, gridHeight = 4, diagonalsAllowed = true)
        val start = graph.node(Vector2Int(0, 0))!!
        val goal = graph.node(Vector2Int(3, 3))!!

        val path = graph.findPath(start, goal)

        assertEquals(start, path.first())
        assertEquals(goal, path.last())
        // Diagonal movement lets the path reach the opposite corner in 3 steps (4 nodes).
        assertEquals(4, path.size)
    }
}
