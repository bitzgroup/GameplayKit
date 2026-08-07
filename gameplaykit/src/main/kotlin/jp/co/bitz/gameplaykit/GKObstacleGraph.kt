package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

private const val MIN_COS_HALF_ANGLE = 0.01f

// A GKGraph that builds a visibility graph around polygon obstacles, mirroring GameplayKit's
// GKObstacleGraph: each obstacle's vertices are inflated outward by `bufferRadius` and turned into
// GKGraphNode2D nodes, which are connected to every other node they have unobstructed line-of-sight
// to. `connectNode` links an externally created node (typically a path start/goal) into that mesh.
//
// The vertex-offset used to apply `bufferRadius` assumes convex, non-self-intersecting obstacle
// polygons; GameplayKit's own buffering algorithm is undocumented, so this is not guaranteed to be
// bit-identical, only contract-conformant (paths stay at least `bufferRadius` from obstacle edges).
public class GKObstacleGraph(
    obstacles: List<GKPolygonObstacle> = emptyList(),
    public val bufferRadius: Float = 0f,
) : GKGraph() {
    private val mutableObstacles: MutableList<GKPolygonObstacle> = mutableListOf()
    private val bufferedPolygons: MutableMap<GKPolygonObstacle, List<Vector2>> = mutableMapOf()
    private val obstacleNodes: MutableMap<GKPolygonObstacle, List<GKGraphNode2D>> = mutableMapOf()

    public val obstacles: List<GKPolygonObstacle>
        get() = mutableObstacles.toList()

    init {
        addObstacles(obstacles)
    }

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

    public fun removeObstacles(oldObstacles: List<GKPolygonObstacle>) {
        oldObstacles.forEach { obstacle ->
            mutableObstacles.remove(obstacle)
            bufferedPolygons.remove(obstacle)
            obstacleNodes.remove(obstacle)?.let { remove(it) }
        }
        reconnectAll()
    }

    public fun removeAllObstacles() {
        removeObstacles(mutableObstacles.toList())
    }

    public fun nodes(obstacle: GKPolygonObstacle): List<GKGraphNode2D> = obstacleNodes[obstacle].orEmpty()

    // Registers `node` (if not already in the graph) and connects it to every existing node it has
    // unobstructed line-of-sight to, given the current obstacles.
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

private fun segmentCrossesPolygon(
    a: Vector2,
    b: Vector2,
    polygon: List<Vector2>,
): Boolean {
    for (i in polygon.indices) {
        val edgeStart = polygon[i]
        val edgeEnd = polygon[(i + 1) % polygon.size]
        if (segmentsIntersect(a, b, edgeStart, edgeEnd)) return true
    }
    return false
}

// Proper-intersection test (shared endpoints don't count as crossing), so connecting two adjacent
// vertices of the same obstacle isn't incorrectly treated as blocked by that obstacle's own edge.
private fun segmentsIntersect(
    p1: Vector2,
    p2: Vector2,
    p3: Vector2,
    p4: Vector2,
): Boolean {
    val d1 = cross(p4, p3, p1)
    val d2 = cross(p4, p3, p2)
    val d3 = cross(p2, p1, p3)
    val d4 = cross(p2, p1, p4)
    return ((d1 > 0f && d2 < 0f) || (d1 < 0f && d2 > 0f)) &&
        ((d3 > 0f && d4 < 0f) || (d3 < 0f && d4 > 0f))
}

private fun cross(
    a: Vector2,
    b: Vector2,
    c: Vector2,
): Float = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

// Standard even-odd ray-casting point-in-polygon test.
private fun pointInPolygon(
    point: Vector2,
    polygon: List<Vector2>,
): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val vi = polygon[i]
        val vj = polygon[j]
        if ((vi.y > point.y) != (vj.y > point.y)) {
            val intersectX = (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x
            if (point.x < intersectX) inside = !inside
        }
        j = i
    }
    return inside
}

// Offsets each vertex outward along the bisector of its two adjacent edge normals, scaled so the
// perpendicular distance from each original edge is exactly `radius`.
private fun bufferedPolygon(
    vertices: List<Vector2>,
    radius: Float,
): List<Vector2> {
    if (radius <= 0f || vertices.size < 3) return vertices
    val outwardSign = if (signedArea(vertices) >= 0f) 1f else -1f
    return vertices.indices.map { i ->
        val prev = vertices[(i - 1 + vertices.size) % vertices.size]
        val curr = vertices[i]
        val next = vertices[(i + 1) % vertices.size]
        val n1 = outwardNormal(prev, curr, outwardSign)
        val n2 = outwardNormal(curr, next, outwardSign)
        val bisector = normalize(Vector2(n1.x + n2.x, n1.y + n2.y))
        val cosHalfAngle = dot(bisector, n1)
        val offset = if (cosHalfAngle > MIN_COS_HALF_ANGLE) radius / cosHalfAngle else radius
        Vector2(curr.x + bisector.x * offset, curr.y + bisector.y * offset)
    }
}

private fun signedArea(vertices: List<Vector2>): Float {
    var sum = 0f
    for (i in vertices.indices) {
        val a = vertices[i]
        val b = vertices[(i + 1) % vertices.size]
        sum += a.x * b.y - b.x * a.y
    }
    return sum / 2f
}

private fun outwardNormal(
    a: Vector2,
    b: Vector2,
    outwardSign: Float,
): Vector2 {
    val edge = Vector2(b.x - a.x, b.y - a.y)
    return normalize(Vector2(edge.y * outwardSign, -edge.x * outwardSign))
}

private fun normalize(v: Vector2): Vector2 {
    val length = sqrt(v.x * v.x + v.y * v.y)
    return if (length > 0f) Vector2(v.x / length, v.y / length) else v
}

private fun dot(
    a: Vector2,
    b: Vector2,
): Float = a.x * b.x + a.y * b.y
