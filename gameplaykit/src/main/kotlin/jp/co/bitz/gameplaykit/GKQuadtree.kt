package jp.co.bitz.gameplaykit

/**
 * A data structure that efficiently organizes elements by location in 2D space, mirroring
 * GameplayKit's `GKQuadtree`. Subdivides [boundingQuad] into quadrants on demand, stopping once a
 * quadrant's half-extent would fall below [minimumCellSize]; an element is stored at the deepest
 * quadrant that fully contains it, so ancestor nodes may hold elements too large for their
 * children.
 *
 * Deviates from Apple's `ElementType: AnyObject` constraint by allowing any non-null Kotlin type
 * (including value/data classes), consistent with this library's idiomatic-Kotlin design
 * principle.
 */
public class GKQuadtree<ElementType : Any>(
    public val boundingQuad: GKQuad,
    public val minimumCellSize: Float,
) {
    private val root: GKQuadtreeNode = GKQuadtreeNode(boundingQuad)

    /** Adds [element] at [point], returning the node it was stored in. */
    public fun add(
        element: ElementType,
        point: Vector2,
    ): GKQuadtreeNode = addQuadtreeElement(root, minimumCellSize, element, GKQuad(point, point))

    /** Adds [element] covering [quad], returning the node it was stored in. */
    public fun add(
        element: ElementType,
        quad: GKQuad,
    ): GKQuadtreeNode = addQuadtreeElement(root, minimumCellSize, element, quad)

    /** Returns every element registered at (or covering) [point]. */
    public fun elements(point: Vector2): List<ElementType> =
        quadtreeElementsAt<ElementType>(root, minimumCellSize, GKQuad(point, point))

    /** Returns every element whose registered region intersects [quad]. */
    public fun elements(quad: GKQuad): List<ElementType> {
        val result = mutableListOf<ElementType>()
        collectIntersectingQuadtreeElements(root, quad, result)
        return result
    }

    /** Removes [element] from wherever it's stored in the tree, returning whether it was found. */
    public fun remove(element: ElementType): Boolean = removeFromQuadtree(root, element)

    /** Removes [element] from [node] specifically, returning whether it was found there. */
    public fun remove(
        element: ElementType,
        node: GKQuadtreeNode,
    ): Boolean = removeFromQuadtreeNode(node, element)
}

private fun <ElementType : Any> addQuadtreeElement(
    root: GKQuadtreeNode,
    minimumCellSize: Float,
    element: ElementType,
    quad: GKQuad,
): GKQuadtreeNode {
    var node = root
    while (canSubdivideQuad(node.quad, minimumCellSize)) {
        val quadrants = quadQuadrants(node.quad)
        val index = quadrants.indexOfFirst { it.contains(quad) }
        if (index < 0) break
        val parent = node
        node = parent.children[index] ?: GKQuadtreeNode(quadrants[index]).also { parent.children[index] = it }
    }
    node.elements.add(GKQuadtreeElementRecord(element, quad))
    return node
}

private fun <ElementType : Any> quadtreeElementsAt(
    root: GKQuadtreeNode,
    minimumCellSize: Float,
    pointQuad: GKQuad,
): List<ElementType> {
    val result = mutableListOf<ElementType>()
    var node: GKQuadtreeNode? = root
    while (node != null) {
        node.elements.forEach { record ->
            if (record.quad.contains(pointQuad)) result.add(castQuadtreeElement(record.element))
        }
        node =
            if (canSubdivideQuad(node.quad, minimumCellSize)) {
                val quadrants = quadQuadrants(node.quad)
                val index = quadrants.indexOfFirst { it.contains(pointQuad) }
                if (index >= 0) node.children[index] else null
            } else {
                null
            }
    }
    return result
}

private fun <ElementType : Any> collectIntersectingQuadtreeElements(
    node: GKQuadtreeNode,
    searchQuad: GKQuad,
    result: MutableList<ElementType>,
) {
    if (!node.quad.intersects(searchQuad)) return
    node.elements.forEach { record ->
        if (record.quad.intersects(searchQuad)) result.add(castQuadtreeElement(record.element))
    }
    node.children.forEach { child -> child?.let { collectIntersectingQuadtreeElements(it, searchQuad, result) } }
}

private fun <ElementType : Any> removeFromQuadtree(
    node: GKQuadtreeNode,
    element: ElementType,
): Boolean {
    if (removeFromQuadtreeNode(node, element)) return true
    return node.children.any { child -> child != null && removeFromQuadtree(child, element) }
}

private fun <ElementType : Any> removeFromQuadtreeNode(
    node: GKQuadtreeNode,
    element: ElementType,
): Boolean {
    val record = node.elements.find { it.element == element } ?: return false
    node.elements.remove(record)
    return true
}

private fun canSubdivideQuad(
    quad: GKQuad,
    minimumCellSize: Float,
): Boolean {
    val halfWidth = (quad.max.x - quad.min.x) / 2f
    val halfHeight = (quad.max.y - quad.min.y) / 2f
    return halfWidth >= minimumCellSize && halfHeight >= minimumCellSize
}

// Splits a quad into its four quadrants in a fixed order (SW, SE, NW, NE), matching the child
// slot indices used in GKQuadtreeNode.children.
private fun quadQuadrants(quad: GKQuad): Array<GKQuad> {
    val midX = (quad.min.x + quad.max.x) / 2f
    val midY = (quad.min.y + quad.max.y) / 2f
    return arrayOf(
        GKQuad(Vector2(quad.min.x, quad.min.y), Vector2(midX, midY)),
        GKQuad(Vector2(midX, quad.min.y), Vector2(quad.max.x, midY)),
        GKQuad(Vector2(quad.min.x, midY), Vector2(midX, quad.max.y)),
        GKQuad(Vector2(midX, midY), Vector2(quad.max.x, quad.max.y)),
    )
}

@Suppress("UNCHECKED_CAST")
private fun <ElementType : Any> castQuadtreeElement(element: Any): ElementType = element as ElementType
