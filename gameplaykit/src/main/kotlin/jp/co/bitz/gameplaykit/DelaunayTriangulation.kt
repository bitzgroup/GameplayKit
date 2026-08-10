package jp.co.bitz.gameplaykit

// The classic Bowyer-Watson algorithm for computing a Delaunay triangulation of a 2D point set,
// used internally by GKMeshGraph.triangulate(). GameplayKit doesn't document its own
// triangulation algorithm, so the resulting triangle boundaries are contract-conformant (a valid
// Delaunay triangulation of the input points), not necessarily bit-identical.
//
// Returns each triangle as (i, j, k) indices into `points`, wound counterclockwise.
internal fun delaunayTriangulate(points: List<Vector2>): List<Triple<Int, Int, Int>> {
    if (points.size < 3) return emptyList()

    val superTriangle = superTriangleAround(points)
    val allPoints = points + superTriangle
    val superIndices = setOf(points.size, points.size + 1, points.size + 2)

    val triangles = mutableListOf(orderedTriangle(allPoints, points.size, points.size + 1, points.size + 2))
    points.indices.forEach { pointIndex -> insertPoint(allPoints, triangles, pointIndex) }

    return triangles.filter { (i, j, k) -> i !in superIndices && j !in superIndices && k !in superIndices }
}

private fun insertPoint(
    allPoints: List<Vector2>,
    triangles: MutableList<Triple<Int, Int, Int>>,
    pointIndex: Int,
) {
    val p = allPoints[pointIndex]
    val badTriangles = triangles.filter { (i, j, k) -> inCircumcircle(allPoints[i], allPoints[j], allPoints[k], p) }
    val boundary = boundaryEdges(badTriangles)
    triangles.removeAll(badTriangles)
    boundary.forEach { (i, j) -> triangles.add(orderedTriangle(allPoints, i, j, pointIndex)) }
}

// A triangle several times larger than the point set's bounding box, guaranteed to contain every
// point — Bowyer-Watson starts from this single triangle and inserts points one at a time.
private fun superTriangleAround(points: List<Vector2>): List<Vector2> {
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val delta = maxOf(maxX - minX, maxY - minY).coerceAtLeast(1f) * SUPER_TRIANGLE_SCALE
    val midX = (minX + maxX) / 2f
    val midY = (minY + maxY) / 2f

    return listOf(
        Vector2(midX - 2 * delta, midY - delta),
        Vector2(midX, midY + 2 * delta),
        Vector2(midX + 2 * delta, midY - delta),
    )
}

private fun orderedTriangle(
    points: List<Vector2>,
    i: Int,
    j: Int,
    k: Int,
): Triple<Int, Int, Int> {
    val a = points[i]
    val b = points[j]
    val c = points[k]
    val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    return if (area >= 0f) Triple(i, j, k) else Triple(i, k, j)
}

// True if `p` lies inside the circumcircle of the (counterclockwise-wound) triangle (a, b, c).
private fun inCircumcircle(
    a: Vector2,
    b: Vector2,
    c: Vector2,
    p: Vector2,
): Boolean {
    val ax = a.x - p.x
    val ay = a.y - p.y
    val bx = b.x - p.x
    val by = b.y - p.y
    val cx = c.x - p.x
    val cy = c.y - p.y
    val det =
        (ax * ax + ay * ay) * (bx * cy - cx * by) -
            (bx * bx + by * by) * (ax * cy - cx * ay) +
            (cx * cx + cy * cy) * (ax * by - bx * ay)
    return det > 0f
}

// The edges of the "hole" left by removing `badTriangles`: exactly the edges that belong to only
// one bad triangle (edges shared by two bad triangles are interior to the hole and discarded).
private fun boundaryEdges(badTriangles: List<Triple<Int, Int, Int>>): List<Pair<Int, Int>> {
    val edgeCounts = mutableMapOf<Pair<Int, Int>, Int>()
    badTriangles.forEach { (i, j, k) ->
        listOf(i to j, j to k, k to i).forEach { (a, b) ->
            val key = if (a < b) a to b else b to a
            edgeCounts[key] = (edgeCounts[key] ?: 0) + 1
        }
    }
    return edgeCounts.filterValues { it == 1 }.keys.toList()
}

private const val SUPER_TRIANGLE_SCALE = 20f
