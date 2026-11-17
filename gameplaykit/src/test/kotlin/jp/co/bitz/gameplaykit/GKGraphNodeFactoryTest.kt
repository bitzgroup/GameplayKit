package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class CustomGridNode(
    gridPosition: Vector2Int,
) : GKGridGraphNode(gridPosition)

private class CustomNode2D(
    position: Vector2,
) : GKGraphNode2D(position)

/**
 * Mirrors the tic-tac-toe sample's `GKCustomNodeClassParityTests` probe of Apple's `nodeClass:`
 * graph initializers.
 */
class GKGraphNodeFactoryTest {
    private fun square() =
        GKPolygonObstacle(
            listOf(Vector2(-2f, -2f), Vector2(2f, -2f), Vector2(2f, 2f), Vector2(-2f, 2f)),
        )

    @Test
    fun `a grid graph builds every node with the factory`() {
        val positions = mutableListOf<Vector2Int>()
        val graph =
            GKGridGraph(Vector2Int(0, 0), 2, 3, diagonalsAllowed = false) { position ->
                positions += position
                CustomGridNode(position)
            }
        assertEquals(6, graph.nodes.size)
        assertTrue(graph.nodes.all { it is CustomGridNode })
        assertEquals(6, positions.toSet().size)
        assertTrue(graph.node(Vector2Int(1, 2)) is CustomGridNode)
    }

    @Test
    fun `a grid graph builds plain nodes by default`() {
        val graph = GKGridGraph(Vector2Int(0, 0), 2, 2, diagonalsAllowed = false)
        assertTrue(graph.nodes.all { it::class == GKGridGraphNode::class })
    }

    @Test
    fun `a plain node can join a grid of a custom class`() {
        val graph = GKGridGraph(Vector2Int(0, 0), 2, 1, diagonalsAllowed = false, nodeFactory = ::CustomGridNode)
        val plain = GKGridGraphNode(Vector2Int(2, 0))
        graph.connectNodeToAdjacentNodes(plain)
        assertTrue(plain in graph.nodes)
        assertEquals(1, plain.connectedNodes.size)
    }

    @Test
    fun `a mesh graph triangulates into nodes from the factory`() {
        var built = 0
        val graph =
            GKMeshGraph(1f, Vector2(-10f, -10f), Vector2(10f, 10f)) { position ->
                built++
                CustomNode2D(position)
            }
        graph.addObstacles(listOf(square()))
        graph.triangulate()
        assertTrue(graph.nodes.isNotEmpty())
        assertTrue(graph.nodes.all { it is CustomNode2D })
        assertEquals(graph.nodes.size, built)

        val plain = GKMeshGraph(1f, Vector2(-10f, -10f), Vector2(10f, 10f))
        plain.addObstacles(listOf(square()))
        plain.triangulate()
        assertTrue(plain.nodes.all { it::class == GKGraphNode2D::class })
    }

    @Test
    fun `a mesh graph uses the factory for every triangulation mode`() {
        val graph = GKMeshGraph(1f, Vector2(-10f, -10f), Vector2(10f, 10f), ::CustomNode2D)
        graph.triangulationMode = GKMeshGraphTriangulationMode.entries.toSet()
        graph.addObstacles(listOf(square()))
        graph.triangulate()
        assertTrue(graph.nodes.all { it is CustomNode2D })
    }

    @Test
    fun `an obstacle graph builds its obstacle nodes from the factory`() {
        val obstacle = square()
        val graph = GKObstacleGraph(listOf(obstacle), 1f, ::CustomNode2D)
        assertEquals(4, graph.nodes(obstacle).size)
        assertTrue(graph.nodes.all { it is CustomNode2D })
    }
}
