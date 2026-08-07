package jp.co.bitz.gameplaykit

// A data structure that efficiently organizes elements by location in 3D space, mirroring
// GameplayKit's GKOctree. Subdivides `boundingBox` into octants on demand, stopping once an octant's
// half-extent would fall below `minimumCellSize`; an element is stored at the deepest octant that
// fully contains it, so ancestor nodes may hold elements too large for their children.
//
// Deviates from Apple's `ElementType: AnyObject` constraint by allowing any non-null Kotlin type
// (including value/data classes), consistent with this library's idiomatic-Kotlin design principle.
public class GKOctree<ElementType : Any>(
    public val boundingBox: GKBox,
    public val minimumCellSize: Float,
) {
    private val root: GKOctreeNode = GKOctreeNode(boundingBox)

    public fun add(
        element: ElementType,
        point: Vector3,
    ): GKOctreeNode = addOctreeElement(root, minimumCellSize, element, GKBox(point, point))

    public fun add(
        element: ElementType,
        box: GKBox,
    ): GKOctreeNode = addOctreeElement(root, minimumCellSize, element, box)

    public fun elements(point: Vector3): List<ElementType> =
        octreeElementsAt<ElementType>(root, minimumCellSize, GKBox(point, point))

    public fun elements(box: GKBox): List<ElementType> {
        val result = mutableListOf<ElementType>()
        collectIntersectingOctreeElements(root, box, result)
        return result
    }

    public fun remove(element: ElementType): Boolean = removeFromOctree(root, element)

    public fun remove(
        element: ElementType,
        node: GKOctreeNode,
    ): Boolean = removeFromOctreeNode(node, element)
}

private fun <ElementType : Any> addOctreeElement(
    root: GKOctreeNode,
    minimumCellSize: Float,
    element: ElementType,
    box: GKBox,
): GKOctreeNode {
    var node = root
    while (canSubdivideBox(node.box, minimumCellSize)) {
        val octants = boxOctants(node.box)
        val index = octants.indexOfFirst { it.contains(box) }
        if (index < 0) break
        val parent = node
        node = parent.children[index] ?: GKOctreeNode(octants[index]).also { parent.children[index] = it }
    }
    node.elements.add(GKOctreeElementRecord(element, box))
    return node
}

private fun <ElementType : Any> octreeElementsAt(
    root: GKOctreeNode,
    minimumCellSize: Float,
    pointBox: GKBox,
): List<ElementType> {
    val result = mutableListOf<ElementType>()
    var node: GKOctreeNode? = root
    while (node != null) {
        node.elements.forEach { record ->
            if (record.box.contains(pointBox)) result.add(castOctreeElement(record.element))
        }
        node =
            if (canSubdivideBox(node.box, minimumCellSize)) {
                val octants = boxOctants(node.box)
                val index = octants.indexOfFirst { it.contains(pointBox) }
                if (index >= 0) node.children[index] else null
            } else {
                null
            }
    }
    return result
}

private fun <ElementType : Any> collectIntersectingOctreeElements(
    node: GKOctreeNode,
    searchBox: GKBox,
    result: MutableList<ElementType>,
) {
    if (!node.box.intersects(searchBox)) return
    node.elements.forEach { record ->
        if (record.box.intersects(searchBox)) result.add(castOctreeElement(record.element))
    }
    node.children.forEach { child -> child?.let { collectIntersectingOctreeElements(it, searchBox, result) } }
}

private fun <ElementType : Any> removeFromOctree(
    node: GKOctreeNode,
    element: ElementType,
): Boolean {
    if (removeFromOctreeNode(node, element)) return true
    return node.children.any { child -> child != null && removeFromOctree(child, element) }
}

private fun <ElementType : Any> removeFromOctreeNode(
    node: GKOctreeNode,
    element: ElementType,
): Boolean {
    val record = node.elements.find { it.element == element } ?: return false
    node.elements.remove(record)
    return true
}

private fun canSubdivideBox(
    box: GKBox,
    minimumCellSize: Float,
): Boolean {
    val halfWidth = (box.max.x - box.min.x) / 2f
    val halfHeight = (box.max.y - box.min.y) / 2f
    val halfDepth = (box.max.z - box.min.z) / 2f
    return halfWidth >= minimumCellSize && halfHeight >= minimumCellSize && halfDepth >= minimumCellSize
}

// Splits a box into its eight octants in a fixed order (bottom four SW/SE/NW/NE, then top four),
// matching the child slot indices used in GKOctreeNode.children.
private fun boxOctants(box: GKBox): Array<GKBox> {
    val midX = (box.min.x + box.max.x) / 2f
    val midY = (box.min.y + box.max.y) / 2f
    val midZ = (box.min.z + box.max.z) / 2f
    return arrayOf(
        GKBox(Vector3(box.min.x, box.min.y, box.min.z), Vector3(midX, midY, midZ)),
        GKBox(Vector3(midX, box.min.y, box.min.z), Vector3(box.max.x, midY, midZ)),
        GKBox(Vector3(box.min.x, midY, box.min.z), Vector3(midX, box.max.y, midZ)),
        GKBox(Vector3(midX, midY, box.min.z), Vector3(box.max.x, box.max.y, midZ)),
        GKBox(Vector3(box.min.x, box.min.y, midZ), Vector3(midX, midY, box.max.z)),
        GKBox(Vector3(midX, box.min.y, midZ), Vector3(box.max.x, midY, box.max.z)),
        GKBox(Vector3(box.min.x, midY, midZ), Vector3(midX, box.max.y, box.max.z)),
        GKBox(Vector3(midX, midY, midZ), Vector3(box.max.x, box.max.y, box.max.z)),
    )
}

@Suppress("UNCHECKED_CAST")
private fun <ElementType : Any> castOctreeElement(element: Any): ElementType = element as ElementType
