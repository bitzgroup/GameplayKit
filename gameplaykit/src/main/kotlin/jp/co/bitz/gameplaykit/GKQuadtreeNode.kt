package jp.co.bitz.gameplaykit

private const val QUADRANT_COUNT = 4

/**
 * A node in a [GKQuadtree]. Automatically created and managed by `GKQuadtree` as elements are
 * added and removed; the public surface only exposes the node's bounding quad, matching
 * GameplayKit's opaque node handle (`GKQuadtreeNode` carries no publicly documented behavior of
 * its own).
 */
public class GKQuadtreeNode internal constructor(
    public val quad: GKQuad,
) {
    internal val children: Array<GKQuadtreeNode?> = arrayOfNulls(QUADRANT_COUNT)
    internal val elements: MutableList<GKQuadtreeElementRecord> = mutableListOf()
}

// An element paired with the quad it was registered under (a point is stored as a zero-area quad),
// so queries can test the element's own region rather than just the bucket node's region.
internal class GKQuadtreeElementRecord(
    val element: Any,
    val quad: GKQuad,
)
