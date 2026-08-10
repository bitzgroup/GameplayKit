package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GKMeshGraphTest {
    @Test
    fun `triangulate fills an empty bounding box with at least one triangle`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))

        graph.triangulate()

        assertTrue(graph.triangleCount > 0)
        assertTrue(graph.nodes.isNotEmpty())
    }

    @Test
    fun `every triangle's vertices lie within the bounding box`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))

        graph.triangulate()

        for (i in 0 until graph.triangleCount) {
            val triangle = graph.getTriangle(at = i)
            triangle.vertices.forEach { vertex ->
                assertTrue(vertex.x in 0f..10f)
                assertTrue(vertex.y in 0f..10f)
            }
        }
    }

    @Test
    fun `vertex mode connects nodes into a single walkable mesh`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.triangulationMode = setOf(GKMeshGraphTriangulationMode.VERTICES)

        graph.triangulate()

        val corners = graph.nodes.filterIsInstance<GKGraphNode2D>()
        val start = corners.minByOrNull { it.position.x + it.position.y }
        val end = corners.maxByOrNull { it.position.x + it.position.y }
        assertNotEquals(null, start)
        assertNotEquals(null, end)
        assertTrue(start!!.pathTo(end!!).isNotEmpty())
    }

    @Test
    fun `no walkable triangle's centroid falls inside an obstacle`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.addObstacles(
            listOf(
                GKPolygonObstacle(
                    vertices =
                        listOf(
                            Vector2(4f, 4f),
                            Vector2(6f, 4f),
                            Vector2(6f, 6f),
                            Vector2(4f, 6f),
                        ),
                ),
            ),
        )

        graph.triangulate()

        for (i in 0 until graph.triangleCount) {
            val triangle = graph.getTriangle(at = i)
            val centroidX = (triangle.a.x + triangle.b.x + triangle.c.x) / 3f
            val centroidY = (triangle.a.y + triangle.b.y + triangle.c.y) / 3f
            assertTrue(!(centroidX in 4f..6f && centroidY in 4f..6f))
        }
    }

    @Test
    fun `a path around an obstacle can still be found`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.addObstacles(
            listOf(
                GKPolygonObstacle(
                    vertices =
                        listOf(
                            Vector2(4f, 0f),
                            Vector2(6f, 0f),
                            Vector2(6f, 8f),
                            Vector2(4f, 8f),
                        ),
                ),
            ),
        )
        graph.triangulate()

        val nodes = graph.nodes.filterIsInstance<GKGraphNode2D>()
        val left = nodes.minByOrNull { it.position.x }
        val right = nodes.maxByOrNull { it.position.x }
        assertNotEquals(null, left)
        assertNotEquals(null, right)
        assertTrue(left!!.pathTo(right!!).isNotEmpty())
    }

    @Test
    fun `centers mode places one node per walkable triangle`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.triangulationMode = setOf(GKMeshGraphTriangulationMode.CENTERS)

        graph.triangulate()

        assertTrue(graph.nodes.size == graph.triangleCount)
    }

    @Test
    fun `connectNodeUsingObstacles wires an external node into the mesh`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.triangulate()
        val nodeCountBefore = graph.nodes.size

        val external = GKGraphNode2D(Vector2(5f, 5f))
        graph.connectNodeUsingObstacles(external)

        assertTrue(graph.nodes.size == nodeCountBefore + 1)
        assertTrue(external.connectedNodes.isNotEmpty())
    }

    @Test
    fun `removeAllObstacles clears the obstacle list`() {
        val graph = GKMeshGraph(bufferRadius = 0f, min = Vector2(0f, 0f), max = Vector2(10f, 10f))
        graph.addObstacles(
            listOf(GKPolygonObstacle(vertices = listOf(Vector2(1f, 1f), Vector2(2f, 1f), Vector2(2f, 2f)))),
        )

        graph.removeAllObstacles()

        assertTrue(graph.obstacles.isEmpty())
    }
}
