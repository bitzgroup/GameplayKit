package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GKObstacleGraphTest {
    private fun rectangleObstacle(): GKPolygonObstacle =
        GKPolygonObstacle(listOf(Vector2(2f, -2f), Vector2(2f, 2f), Vector2(6f, 2f), Vector2(6f, -2f)))

    @Test
    fun `addObstacles and removeObstacles update the obstacles registry and its graph nodes`() {
        val obstacle = GKPolygonObstacle(listOf(Vector2(0f, 0f), Vector2(1f, 0f), Vector2(0f, 1f)))
        val graph = GKObstacleGraph()

        graph.addObstacles(listOf(obstacle))
        assertEquals(listOf(obstacle), graph.obstacles)
        assertEquals(3, graph.nodes(obstacle).size)
        assertTrue(graph.nodes.containsAll(graph.nodes(obstacle)))

        graph.removeObstacles(listOf(obstacle))
        assertEquals(emptyList(), graph.obstacles)
        assertEquals(emptyList(), graph.nodes(obstacle))
        assertTrue(graph.nodes.isEmpty())
    }

    @Test
    fun `connectNode links nodes with a clear line of sight`() {
        val graph = GKObstacleGraph()
        val a = GKGraphNode2D(Vector2(0f, 10f))
        val b = GKGraphNode2D(Vector2(8f, 10f))

        graph.connectNode(a)
        graph.connectNode(b)

        assertTrue(a.connectedNodes.contains(b))
    }

    @Test
    fun `connectNode does not link nodes whose segment crosses an obstacle`() {
        val graph = GKObstacleGraph(listOf(rectangleObstacle()), bufferRadius = 0f)
        val a = GKGraphNode2D(Vector2(0f, 0f))
        val b = GKGraphNode2D(Vector2(8f, 0f))

        graph.connectNode(a)
        graph.connectNode(b)

        assertFalse(a.connectedNodes.contains(b))
    }

    @Test
    fun `findPath routes around an obstacle rather than passing through it`() {
        val graph = GKObstacleGraph(listOf(rectangleObstacle()), bufferRadius = 0f)
        val start = GKGraphNode2D(Vector2(0f, 0f))
        val goal = GKGraphNode2D(Vector2(8f, 0f))
        graph.connectNode(start)
        graph.connectNode(goal)

        val path = graph.findPath(start, goal)

        assertEquals(start, path.first())
        assertEquals(goal, path.last())
        assertTrue(path.size > 2)
    }

    @Test
    fun `a larger bufferRadius blocks routes that a smaller one leaves clear`() {
        val obstacle = rectangleObstacle()
        val start = Vector2(0f, 3f)
        val goal = Vector2(8f, 3f)

        val unbuffered = GKObstacleGraph(listOf(obstacle), bufferRadius = 0f)
        val a1 = GKGraphNode2D(start)
        val b1 = GKGraphNode2D(goal)
        unbuffered.connectNode(a1)
        unbuffered.connectNode(b1)
        assertTrue(a1.connectedNodes.contains(b1))

        val buffered = GKObstacleGraph(listOf(obstacle), bufferRadius = 3f)
        val a2 = GKGraphNode2D(start)
        val b2 = GKGraphNode2D(goal)
        buffered.connectNode(a2)
        buffered.connectNode(b2)
        assertFalse(a2.connectedNodes.contains(b2))
    }
}
