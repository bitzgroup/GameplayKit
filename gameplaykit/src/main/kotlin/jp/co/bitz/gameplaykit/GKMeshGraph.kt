package jp.co.bitz.gameplaykit

/**
 * Controls which kind of node [GKMeshGraph.triangulate] creates for each walkable triangle,
 * mirroring GameplayKit's `GKMeshGraphTriangulationMode` option set (which can combine multiple
 * modes at once — represented here as a Kotlin `Set` for the same effect, idiomatically).
 */
public enum class GKMeshGraphTriangulationMode {
    VERTICES,
    CENTERS,
    EDGES,
}

/** A single triangle produced by [GKMeshGraph.triangulate], mirroring GameplayKit's `GKTriangle`. */
public data class GKTriangle(
    public val a: Vector2,
    public val b: Vector2,
    public val c: Vector2,
) {
    internal val centroid: Vector2
        get() = Vector2((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)

    internal val vertices: List<Vector2>
        get() = listOf(a, b, c)
}

/**
 * A [GKGraph] that fills the navigable space between polygon obstacles with a
 * Delaunay-triangulated mesh, mirroring GameplayKit's `GKMeshGraph` — a more space-efficient
 * alternative to [GKObstacleGraph]'s visibility graph for large or densely-obstacled worlds. Call
 * [addObstacles] then [triangulate] to (re)build the mesh; [triangulationMode] controls what
 * `triangulate()` populates the graph with.
 *
 * Deviation from GameplayKit: not generic over a custom node subclass (Apple's
 * `GKMeshGraph<NodeType>` lets you supply your own `GKGraphNode2D` subclass via a Class/Type
 * argument, using Obj-C's dynamic object creation); this always builds plain [GKGraphNode2D]
 * nodes, matching the non-generic choice already made for [GKObstacleGraph] in this port.
 * Triangulation uses the classic Bowyer-Watson algorithm (see `DelaunayTriangulation.kt`) since
 * GameplayKit doesn't document its own, so triangle boundaries are contract-conformant, not
 * bit-identical.
 */
public class GKMeshGraph(
    public val bufferRadius: Float,
    public val min: Vector2,
    public val max: Vector2,
) : GKGraph() {
    /**
     * Which kind of node [triangulate] populates the graph with. Defaults to
     * [GKMeshGraphTriangulationMode.VERTICES].
     */
    public var triangulationMode: Set<GKMeshGraphTriangulationMode> = setOf(GKMeshGraphTriangulationMode.VERTICES)

    private val mutableObstacles: MutableList<GKPolygonObstacle> = mutableListOf()
    private var walkableTriangles: List<GKTriangle> = emptyList()
    private var bufferedPolygons: List<List<Vector2>> = emptyList()

    /** The obstacles currently registered with this graph. */
    public val obstacles: List<GKPolygonObstacle>
        get() = mutableObstacles.toList()

    /** The number of walkable triangles produced by the last [triangulate] call. */
    public val triangleCount: Int
        get() = walkableTriangles.size

    /** Returns the walkable triangle at [at] (`0 until triangleCount`). */
    public fun getTriangle(at: Int): GKTriangle = walkableTriangles[at]

    /** Registers [newObstacles]. Call [triangulate] afterward to rebuild the mesh around them. */
    public fun addObstacles(newObstacles: List<GKPolygonObstacle>) {
        mutableObstacles.addAll(newObstacles)
    }

    /** Unregisters [oldObstacles]. Call [triangulate] afterward to rebuild the mesh without them. */
    public fun removeObstacles(oldObstacles: List<GKPolygonObstacle>) {
        mutableObstacles.removeAll(oldObstacles.toSet())
    }

    /** Unregisters every obstacle. Call [triangulate] afterward to rebuild the mesh without them. */
    public fun removeAllObstacles() {
        mutableObstacles.clear()
    }

    /**
     * Rebuilds the mesh: triangulates the bounding box against the current obstacles, discards
     * any triangle whose centroid falls inside a buffered obstacle, and repopulates the graph
     * with nodes/connections per [triangulationMode].
     */
    public fun triangulate() {
        remove(nodes)
        bufferedPolygons = mutableObstacles.map { bufferedPolygon(it.vertices, bufferRadius) }
        val corners = listOf(min, Vector2(max.x, min.y), max, Vector2(min.x, max.y))
        val points = (corners + bufferedPolygons.flatten()).distinct()
        walkableTriangles =
            delaunayTriangulate(points)
                .map { (i, j, k) -> GKTriangle(points[i], points[j], points[k]) }
                .filter { triangle -> bufferedPolygons.none { pointInPolygon(triangle.centroid, it) } }
        buildNodes()
    }

    /**
     * Registers [node] (if not already in the graph) and connects it to every mesh node it has
     * unobstructed line-of-sight to, mirroring GameplayKit's `connectNode(usingObstacles:)`.
     */
    public fun connectNodeUsingObstacles(node: GKGraphNode2D) {
        if (node !in nodes) add(listOf(node))
        nodes.filterIsInstance<GKGraphNode2D>().forEach { other ->
            if (other !== node && !segmentBlockedByObstacle(node.position, other.position)) {
                node.addConnections(listOf(other), bidirectional = true)
            }
        }
    }

    private fun segmentBlockedByObstacle(
        a: Vector2,
        b: Vector2,
    ): Boolean {
        if (bufferedPolygons.any { segmentCrossesPolygon(a, b, it) }) return true
        val midpoint = Vector2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        return bufferedPolygons.any { pointInPolygon(midpoint, it) }
    }

    private fun buildNodes() {
        val adjacency = triangleAdjacency()
        if (GKMeshGraphTriangulationMode.VERTICES in triangulationMode) buildVertexNodes()
        if (GKMeshGraphTriangulationMode.CENTERS in triangulationMode) buildCenterNodes(adjacency)
        if (GKMeshGraphTriangulationMode.EDGES in triangulationMode) buildEdgeNodes(adjacency)
    }

    private fun triangleAdjacency(): List<Pair<Int, Int>> {
        val adjacency = mutableListOf<Pair<Int, Int>>()
        for (i in walkableTriangles.indices) {
            for (j in i + 1 until walkableTriangles.size) {
                if (sharedEdge(walkableTriangles[i], walkableTriangles[j]) != null) adjacency.add(i to j)
            }
        }
        return adjacency
    }

    private fun buildVertexNodes() {
        val nodeByPosition = mutableMapOf<Vector2, GKGraphNode2D>()
        walkableTriangles.forEach { triangle ->
            triangle.vertices.forEach { vertex -> nodeByPosition.getOrPut(vertex) { GKGraphNode2D(vertex) } }
        }
        add(nodeByPosition.values.toList())
        walkableTriangles.forEach {
                triangle ->
            connectTriangleEdges(triangle.vertices.map { nodeByPosition.getValue(it) })
        }
    }

    private fun buildCenterNodes(adjacency: List<Pair<Int, Int>>) {
        val centerNodes = walkableTriangles.map { GKGraphNode2D(it.centroid) }
        add(centerNodes)
        adjacency.forEach { (i, j) -> centerNodes[i].addConnections(listOf(centerNodes[j]), bidirectional = true) }
    }

    // One node per shared edge between two adjacent walkable triangles, at its midpoint; nodes
    // belonging to the same triangle are connected to each other, so a path can flow from one
    // edge of a triangle to another.
    private fun buildEdgeNodes(adjacency: List<Pair<Int, Int>>) {
        val edgeNodeByAdjacency =
            adjacency.associateWith { (i, j) ->
                val edge = checkNotNull(sharedEdge(walkableTriangles[i], walkableTriangles[j]))
                GKGraphNode2D(Vector2((edge.first.x + edge.second.x) / 2f, (edge.first.y + edge.second.y) / 2f))
            }
        add(edgeNodeByAdjacency.values.toList())
        connectEdgeNodesSharingATriangle(edgeNodeByAdjacency)
    }

    private fun connectEdgeNodesSharingATriangle(edgeNodeByAdjacency: Map<Pair<Int, Int>, GKGraphNode2D>) {
        val edgeNodesByTriangle = mutableMapOf<Int, MutableList<GKGraphNode2D>>()
        edgeNodeByAdjacency.forEach { (pair, node) ->
            edgeNodesByTriangle.getOrPut(pair.first) { mutableListOf() }.add(node)
            edgeNodesByTriangle.getOrPut(pair.second) { mutableListOf() }.add(node)
        }
        edgeNodesByTriangle.values.forEach { connectTriangleEdges(it) }
    }

    private fun connectTriangleEdges(triangleNodes: List<GKGraphNode2D>) {
        for (i in triangleNodes.indices) {
            for (j in i + 1 until triangleNodes.size) {
                triangleNodes[i].addConnections(listOf(triangleNodes[j]), bidirectional = true)
            }
        }
    }
}

private fun sharedEdge(
    t1: GKTriangle,
    t2: GKTriangle,
): Pair<Vector2, Vector2>? {
    val verticesB = t2.vertices.toSet()
    val shared = t1.vertices.filter { it in verticesB }
    return if (shared.size == 2) shared[0] to shared[1] else null
}
