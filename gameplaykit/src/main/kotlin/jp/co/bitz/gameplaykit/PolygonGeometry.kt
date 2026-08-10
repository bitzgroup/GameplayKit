package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

// 2D polygon geometry helpers shared by GKObstacleGraph (visibility-graph line-of-sight tests)
// and GKMeshGraph (navmesh triangle culling), factored out here rather than duplicated.

private const val MIN_COS_HALF_ANGLE = 0.01f

// Offsets each vertex outward along the bisector of its two adjacent edge normals, scaled so the
// perpendicular distance from each original edge is exactly `radius`.
internal fun bufferedPolygon(
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

internal fun signedArea(vertices: List<Vector2>): Float {
    var sum = 0f
    for (i in vertices.indices) {
        val a = vertices[i]
        val b = vertices[(i + 1) % vertices.size]
        sum += a.x * b.y - b.x * a.y
    }
    return sum / 2f
}

internal fun outwardNormal(
    a: Vector2,
    b: Vector2,
    outwardSign: Float,
): Vector2 {
    val edge = Vector2(b.x - a.x, b.y - a.y)
    return normalize(Vector2(edge.y * outwardSign, -edge.x * outwardSign))
}

internal fun normalize(v: Vector2): Vector2 {
    val length = sqrt(v.x * v.x + v.y * v.y)
    return if (length > 0f) Vector2(v.x / length, v.y / length) else v
}

internal fun dot(
    a: Vector2,
    b: Vector2,
): Float = a.x * b.x + a.y * b.y

// Standard even-odd ray-casting point-in-polygon test.
internal fun pointInPolygon(
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

internal fun segmentCrossesPolygon(
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
internal fun segmentsIntersect(
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
