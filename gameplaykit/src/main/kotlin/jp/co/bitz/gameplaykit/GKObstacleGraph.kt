package jp.co.bitz.gameplaykit

/**
 * A [GKGraph] that builds a visibility graph around polygon obstacles, mirroring GameplayKit's
 * `GKObstacleGraph`: each obstacle's vertices are inflated outward by [bufferRadius] and turned
 * into [GKGraphNode2D] nodes, which are connected to every other node they have unobstructed
 * line-of-sight to. [connectUsingObstacles] links an externally created node (typically a path
 * start/goal) into that mesh. Adding or removing obstacles reconnects every node from scratch —
 * those linked in from outside too — except for connections [lockConnection] keeps.
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
    private val lockedConnections: MutableSet<Pair<GKGraphNode, GKGraphNode>> = mutableSetOf()

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
     * Registers [node] (if not already in the graph) and connects it, both ways, to every other
     * node it has unobstructed line-of-sight to — Apple's `connectUsingObstacles(node:)` and
     * `connectUsingObstacles(node:ignoring:)`. Obstacles in [ignoring] don't block anything for
     * this node, so it can see straight through them (verified on iOS/tvOS simulators).
     */
    public fun connectUsingObstacles(
        node: GKGraphNode2D,
        ignoring: List<GKPolygonObstacle> = emptyList(),
    ) {
        connect(node, ignored = ignoring.toSet(), unbuffered = emptySet())
    }

    /**
     * Like [connectUsingObstacles], but [obstacles] block at their own outline rather than their
     * buffered one — Apple's `connectUsingObstacles(node:ignoringBufferRadiusOf:)`. This lets a
     * node inside an obstacle's buffer zone (but outside the obstacle) still connect. Renamed
     * because it would otherwise clash with the `ignoring` overload in Kotlin.
     */
    public fun connectUsingObstaclesIgnoringBufferRadius(
        node: GKGraphNode2D,
        obstacles: List<GKPolygonObstacle>,
    ) {
        connect(node, ignored = emptySet(), unbuffered = obstacles.toSet())
    }

    /** The earlier name of [connectUsingObstacles]. */
    @Deprecated("Renamed to Apple's name.", ReplaceWith("connectUsingObstacles(node)"))
    public fun connectNode(node: GKGraphNode2D) {
        connectUsingObstacles(node)
    }

    /**
     * Keeps the connection from [from] to [to] when obstacles are added or removed — Apple's
     * `lockConnection(from:to:)`. As on Apple (verified on iOS/tvOS simulators), the lock is
     * one-way: an obstacle added across the pair cuts `to`→`from` but keeps `from`→`to`. It only
     * keeps a connection that exists when the graph reconnects; it never creates one.
     */
    public fun lockConnection(
        from: GKGraphNode,
        to: GKGraphNode,
    ) {
        lockedConnections += from to to
    }

    /** Lifts [lockConnection]'s lock — Apple's `unlockConnection(from:to:)`. Changes no connection by itself. */
    public fun unlockConnection(
        from: GKGraphNode,
        to: GKGraphNode,
    ) {
        lockedConnections -= from to to
    }

    /** Whether [lockConnection] locked the connection from [from] to [to] — Apple's `isConnectionLocked(from:to:)`. */
    public fun isConnectionLocked(
        from: GKGraphNode,
        to: GKGraphNode,
    ): Boolean = (from to to) in lockedConnections

    private fun connect(
        node: GKGraphNode2D,
        ignored: Set<GKPolygonObstacle>,
        unbuffered: Set<GKPolygonObstacle>,
    ) {
        if (node !in nodes) add(listOf(node))
        nodes.filterIsInstance<GKGraphNode2D>().forEach { other ->
            if (other !== node && !segmentBlocked(node.position, other.position, ignored, unbuffered)) {
                node.addConnections(listOf(other), bidirectional = true)
            }
        }
    }

    private fun reconnectAll() {
        val graphNodes2D = nodes.filterIsInstance<GKGraphNode2D>()
        val kept = lockedConnections.filter { (from, to) -> to in from.connectedNodes }
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
        kept.forEach { (from, to) -> from.addConnections(listOf(to), bidirectional = false) }
    }

    // Blocked if the segment crosses any buffered obstacle's boundary, or passes through its
    // interior (caught by testing the segment's midpoint, which covers same-polygon diagonals that
    // never cross an edge but still cut through the obstacle).
    private fun segmentBlocked(
        a: Vector2,
        b: Vector2,
        ignored: Set<GKPolygonObstacle> = emptySet(),
        unbuffered: Set<GKPolygonObstacle> = emptySet(),
    ): Boolean {
        val polygons =
            bufferedPolygons.mapNotNull { (obstacle, buffered) ->
                when (obstacle) {
                    in ignored -> null
                    in unbuffered -> obstacle.vertices
                    else -> buffered
                }
            }
        if (polygons.any { polygon -> segmentCrossesPolygon(a, b, polygon) }) return true
        val midpoint = Vector2((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        return polygons.any { polygon -> pointInPolygon(midpoint, polygon) }
    }
}
