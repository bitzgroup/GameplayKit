package jp.co.bitz.gameplaykit

/**
 * A [GKGraph] that builds a visibility graph around polygon obstacles, mirroring GameplayKit's
 * `GKObstacleGraph`: each obstacle's vertices are inflated outward by [bufferRadius] and turned
 * into [GKGraphNode2D] nodes, which are connected to every other node they have unobstructed
 * line-of-sight to. [connectNode] links an externally created node (typically a path start/goal)
 * into that mesh.
 *
 * The vertex-offset used to apply [bufferRadius] assumes convex, non-self-intersecting obstacle
 * polygons; GameplayKit's own buffering algorithm is undocumented, so this is not guaranteed to be
 * bit-identical, only contract-conformant (paths stay at least [bufferRadius] from obstacle edges).
 */
public class GKObstacleGraph(
    obstacles: List<GKPolygonObstacle> = emptyList(),
    public val bufferRadius: Float = 0f,
) : GKGraph() {
    private val mutableObstacles: MutableList<GKPolygonObstacle> = mutableListOf()
    private val bufferedPolygons: MutableMap<GKPolygonObstacle, List<Vector2>> = mutableMapOf()
    private val obstacleNodes: MutableMap<GKPolygonObstacle, List<GKGraphNode2D>> = mutableMapOf()

    /** The obstacles currently registered with this graph. */
    public val obstacles: List<GKPolygonObstacle>
        get() = mutableObstacles.toList()

    init {
        addObstacles(obstacles)
    }

    /** Adds [newObstacles] to the graph and rebuilds the visibility connections around all obstacles. */
    public fun addObstacles(newObstacles: List<GKPolygonObstacle>) {
        newObstacles.forEach { obstacle ->
            mutableObstacles.add(obstacle)
            val buffered = bufferedPolygon(obstacle.vertices, bufferRadius)
            bufferedPolygons[obstacle] = buffered
            val nodes = buffered.map { GKGraphNode2D(it) }
            obstacleNodes[obstacle] = nodes
            add(nodes)
        }
        reconnectAll()
    }

    /** Removes [oldObstacles] from the graph and rebuilds the visibility connections around the rest. */
    public fun removeObstacles(oldObstacles: List<GKPolygonObstacle>) {
        oldObstacles.forEach { obstacle ->
            mutableObstacles.remove(obstacle)
            bufferedPolygons.remove(obstacle)
            obstacleNodes.remove(obstacle)?.let { remove(it) }
        }
        reconnectAll()
    }

    /** Removes every obstacle from the graph. */
    public fun removeAllObstacles() {
        removeObstacles(mutableObstacles.toList())
    }

    /** The buffered [GKGraphNode2D] nodes generated for [obstacle]'s vertices. */
    public fun nodes(obstacle: GKPolygonObstacle): List<GKGraphNode2D> = obstacleNodes[obstacle].orEmpty()

    /**
     * Registers [node] (if not already in the graph) and connects it to every existing node it
     * has unobstructed line-of-sight to, given the current obstacles.
     */
    public fun connectNode(node: GKGraphNode2D) {
        if (node !in nodes) add(listOf(node))
        nodes.filterIsInstance<GKGraphNode2D>().forEach { other ->
            if (other !== node && !segmentBlocked(node.position, other.position)) {
                node.addConnections(listOf(other), bidirectional = true)
            }
        }
    }

    private fun reconnectAll() {
        val graphNodes2D = nodes.filterIsInstance<GKGraphNode2D>()
        graphNodes2D.forEach { it.removeConnections(it.connectedNodes, bidirectional = true) }
        for (i in graphNodes2D.indices) {
            for (j in i + 1 until graphNodes2D.size) {
                val a = graphNodes2D[i]
                val b = graphNodes2D[j]
                if (!segmentBlocked(a.position, b.position)) {
                    a.addConnections(listOf(b), bidirectional = true)
                }
            }
        }
    }

    // Blocked if the segment crosses any buffered obstacle's boundary, or passes through its
    // interior (caught by testing the segment's midpoint, which covers same-polygon diagonals that
    // never cross an edge but still cut through the obstacle).
    private fun segmentBlocked(
        a: Vector2,
        b: Vector2,
    ): Boolean {
        val polygons = bufferedPolygons.values
        if (polygons.any { polygon -> segmentCrossesPolygon(a, b, polygon) }) return true
        val midpoint = Vector2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        return polygons.any { polygon -> pointInPolygon(midpoint, polygon) }
    }
}
