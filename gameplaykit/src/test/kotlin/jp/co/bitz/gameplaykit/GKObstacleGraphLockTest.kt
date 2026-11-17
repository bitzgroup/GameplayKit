package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mirrors tic-tac-toe's `GKObstacleGraphParityTests` (Apple's behavior, recorded on iOS/tvOS simulators). */
class GKObstacleGraphLockTest {
    private fun square(
        cx: Float,
        cy: Float,
        half: Float,
    ) = GKPolygonObstacle(
        listOf(
            Vector2(cx - half, cy - half),
            Vector2(cx + half, cy - half),
            Vector2(cx + half, cy + half),
            Vector2(cx - half, cy + half),
        ),
    )

    private fun GKGraphNode.linksTo(other: GKGraphNode) = connectedNodes.any { it === other }

    private fun List<GKGraphNode2D>.at(
        x: Float,
        y: Float,
    ) = first { kotlin.math.abs(it.position.x - x) < 0.01f && kotlin.math.abs(it.position.y - y) < 0.01f }

    @Test
    fun `a lock keeps only that direction of a connection when an obstacle blocks it`() {
        val left = square(0f, 0f, 10f)
        val right = square(100f, 0f, 10f)
        val graph = GKObstacleGraph(listOf(left, right), bufferRadius = 2f)
        val a = graph.nodes(left).at(12f, -12f)
        val b = graph.nodes(right).at(88f, -12f)
        assertTrue(a.linksTo(b) && b.linksTo(a))
        assertFalse(graph.isConnectionLocked(a, b))

        graph.lockConnection(a, b)
        assertTrue(graph.isConnectionLocked(a, b))
        assertFalse(graph.isConnectionLocked(b, a))

        val middle = square(50f, 0f, 30f)
        graph.addObstacles(listOf(middle))
        assertTrue(a.linksTo(b))
        assertFalse(b.linksTo(a))

        graph.unlockConnection(a, b)
        assertFalse(graph.isConnectionLocked(a, b))
        assertTrue(a.linksTo(b))

        graph.removeObstacles(listOf(middle))
        assertTrue(a.linksTo(b) && b.linksTo(a))
        graph.addObstacles(listOf(middle))
        assertFalse(a.linksTo(b) || b.linksTo(a))
    }

    @Test
    fun `locking a pair that is not connected records the lock but connects nothing`() {
        val graph = GKObstacleGraph(listOf(square(0f, 0f, 10f)))
        val p = GKGraphNode2D(Vector2(-50f, 0f))
        val q = GKGraphNode2D(Vector2(50f, 0f))
        graph.lockConnection(p, q)
        assertTrue(graph.isConnectionLocked(p, q))
        assertFalse(p.linksTo(q))
        assertFalse(p in graph.nodes)
    }

    @Test
    fun `adding an obstacle reconnects nodes connected from outside too`() {
        val graph = GKObstacleGraph(listOf(square(0f, 0f, 10f)))
        val p = GKGraphNode2D(Vector2(-50f, 0f))
        val q = GKGraphNode2D(Vector2(50f, 50f))
        graph.connectUsingObstacles(p)
        graph.connectUsingObstacles(q)
        assertTrue(p.linksTo(q))
        graph.addObstacles(listOf(square(0f, 25f, 10f)))
        assertFalse(p.linksTo(q) || q.linksTo(p))
    }

    @Test
    fun `connecting ignoring obstacles lets the node see through them`() {
        val obstacle = square(50f, 0f, 20f)
        val graph = GKObstacleGraph(listOf(obstacle), bufferRadius = 5f)
        val start = GKGraphNode2D(Vector2(50f, -60f))
        val end = GKGraphNode2D(Vector2(50f, 60f))
        graph.connectUsingObstacles(start)
        graph.connectUsingObstacles(end)
        assertFalse(start.linksTo(end))
        assertEquals(2, start.connectedNodes.size)

        val seeingStart = GKGraphNode2D(Vector2(50f, -60f))
        val seeingEnd = GKGraphNode2D(Vector2(50f, 60f))
        graph.connectUsingObstacles(seeingStart, ignoring = listOf(obstacle))
        graph.connectUsingObstacles(seeingEnd, ignoring = listOf(obstacle))
        assertTrue(seeingStart.linksTo(seeingEnd) && seeingEnd.linksTo(seeingStart))
        assertEquals(7, seeingStart.connectedNodes.size)
    }

    @Test
    fun `connecting ignoring the buffer radius lets a node inside the buffer connect`() {
        val obstacle = square(50f, 0f, 20f)
        val graph = GKObstacleGraph(listOf(obstacle), bufferRadius = 5f)
        graph.connectUsingObstacles(GKGraphNode2D(Vector2(50f, -60f)))
        val inBuffer = GKGraphNode2D(Vector2(50f, -23f))
        graph.connectUsingObstacles(inBuffer)
        assertTrue(inBuffer.connectedNodes.isEmpty())

        val ignoringBuffer = GKGraphNode2D(Vector2(50f, -23f))
        graph.connectUsingObstaclesIgnoringBufferRadius(ignoringBuffer, listOf(obstacle))
        val positions = ignoringBuffer.connectedNodes.map { (it as GKGraphNode2D).position }
        assertEquals(4, positions.size)
        assertTrue(Vector2(50f, -60f) in positions)
        assertTrue(Vector2(50f, -23f) in positions)
        assertTrue(positions.any { kotlin.math.abs(it.x - 25f) < 0.01f && kotlin.math.abs(it.y + 25f) < 0.01f })
        assertTrue(positions.any { kotlin.math.abs(it.x - 75f) < 0.01f && kotlin.math.abs(it.y + 25f) < 0.01f })
    }
}
