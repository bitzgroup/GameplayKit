package jp.co.bitz.gameplaykit

private const val OCTANT_COUNT = 8

/**
 * A node in a [GKOctree]. Automatically created and managed by `GKOctree` as elements are added
 * and removed; the public surface only exposes the node's bounding box, matching GameplayKit's
 * opaque node handle (`GKOctreeNode` carries no publicly documented behavior of its own).
 */
public class GKOctreeNode internal constructor(
    public val box: GKBox,
) {
    internal val children: Array<GKOctreeNode?> = arrayOfNulls(OCTANT_COUNT)
    internal val elements: MutableList<GKOctreeElementRecord> = mutableListOf()
}

// An element paired with the box it was registered under (a point is stored as a zero-volume box),
// so queries can test the element's own region rather than just the bucket node's region.
internal class GKOctreeElementRecord(
    val element: Any,
    val box: GKBox,
)
