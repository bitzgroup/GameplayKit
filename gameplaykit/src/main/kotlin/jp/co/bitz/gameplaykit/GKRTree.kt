package jp.co.bitz.gameplaykit

import kotlin.math.abs

/**
 * How a [GKRTree] node that has overflowed splits its entries between two nodes, mirroring
 * GameplayKit's `GKRTreeSplitStrategy`. The choice only affects the tree's shape (and so query
 * speed and result order), never which elements a query finds.
 */
public enum class GKRTreeSplitStrategy {
    /** Sorts entries along the axis their centers spread over most and cuts the list in half. */
    HALVE,

    /** Guttman's linear-cost split: seeds far apart along one axis, the rest to the group that grows least. */
    LINEAR,

    /**
     * Guttman's quadratic-cost split: seeds that would waste the most area together, then the most
     * decided entry first.
     */
    QUADRATIC,

    /** The R*-tree split: the axis and cut that leave the two nodes overlapping least. */
    REDUCE_OVERLAP,
}

/**
 * A data structure that organizes elements by their bounding rectangles in 2D space, mirroring
 * GameplayKit's `GKRTree` — the third spatial-partitioning structure beside [GKQuadtree] and
 * [GKOctree], suited to elements with large or uneven extents. Each node holds up to
 * [maxNumberOfChildren] entries (at least 2: Apple's crashes with 1); a node that overflows splits
 * per the [GKRTreeSplitStrategy] passed to [add].
 *
 * Behavior verified against Apple's on iOS/tvOS simulators:
 * - [elements] returns only elements whose rect the query rect **fully contains** (edges
 *   included) — not every element it intersects.
 * - The same element can be added any number of times; each add is a separate record.
 * - [remove] looks only inside child nodes whose bounds fully contain the rect passed, and in the
 *   first node holding the element there removes its first record, whatever rect that record has.
 *
 * Deviates from Apple's: result order is this tree's traversal order, which matches Apple's only
 * while every element fits in one node (insertion order) — split strategies are the textbook
 * algorithms, as Apple's aren't documented. Unlike Apple's `ElementType: AnyObject`, any non-null
 * type works, matching [GKQuadtree].
 */
public class GKRTree<ElementType : Any>(
    public val maxNumberOfChildren: Int,
) {
    init {
        require(maxNumberOfChildren >= 2) { "maxNumberOfChildren must be at least 2" }
    }

    /**
     * Apple's hint for how many results a query should reserve room for. Kept for API parity; as on
     * Apple, it never changes what a query returns. Defaults to 1, as Apple's does.
     */
    public var queryReserve: Int = 1

    private val minimumFill: Int = (maxNumberOfChildren + 1) / 2
    private var root: RTreeNode = RTreeNode(isLeaf = true)

    /** Adds [element] covering the rect from [boundingRectMin] to [boundingRectMax]. */
    public fun add(
        element: ElementType,
        boundingRectMin: Vector2,
        boundingRectMax: Vector2,
        splitStrategy: GKRTreeSplitStrategy,
    ) {
        val record = RTreeEntry(RTreeRect(boundingRectMin, boundingRectMax), element = element)
        val path = chooseLeafPath(record.rect, splitStrategy)
        path.last().entries.add(record)
        var splitOff: RTreeNode? = null
        for (index in path.indices.reversed()) {
            val node = path[index]
            splitOff?.let { node.entries.add(RTreeEntry(it.bounds(), child = it)) }
            splitOff = if (node.entries.size > maxNumberOfChildren) split(node, splitStrategy) else null
            if (index > 0) path[index - 1].entries.first { it.child === node }.rect = node.bounds()
        }
        splitOff?.let { sibling ->
            val oldRoot = root
            root = RTreeNode(isLeaf = false)
            root.entries.add(RTreeEntry(oldRoot.bounds(), child = oldRoot))
            root.entries.add(RTreeEntry(sibling.bounds(), child = sibling))
        }
    }

    /**
     * Removes one record of [element], searching only child nodes whose bounds fully contain the
     * rect from [boundingRectMin] to [boundingRectMax] — see the class notes. Does nothing if it
     * isn't found there.
     */
    public fun remove(
        element: ElementType,
        boundingRectMin: Vector2,
        boundingRectMax: Vector2,
    ) {
        removeFrom(root, element, RTreeRect(boundingRectMin, boundingRectMax))
        while (!root.isLeaf && root.entries.size == 1) root = checkNotNull(root.entries[0].child)
        if (!root.isLeaf && root.entries.isEmpty()) root = RTreeNode(isLeaf = true)
    }

    /** Returns every element whose rect lies entirely within the rect from [rectMin] to [rectMax]. */
    public fun elements(
        rectMin: Vector2,
        rectMax: Vector2,
    ): List<ElementType> {
        val query = RTreeRect(rectMin, rectMax)
        val result = mutableListOf<ElementType>()
        collect(root, query, result)
        return result
    }

    private fun chooseLeafPath(
        rect: RTreeRect,
        strategy: GKRTreeSplitStrategy,
    ): List<RTreeNode> {
        val path = mutableListOf(root)
        while (!path.last().isLeaf) {
            val entries = path.last().entries
            val childrenAreLeaves = checkNotNull(entries[0].child).isLeaf
            val best =
                if (strategy == GKRTreeSplitStrategy.REDUCE_OVERLAP && childrenAreLeaves) {
                    entries.minWith(
                        compareBy<RTreeEntry> { overlapEnlargement(it, rect, entries) }
                            .thenBy { it.rect.union(rect).area - it.rect.area }
                            .thenBy { it.rect.area },
                    )
                } else {
                    entries.minWith(
                        compareBy<RTreeEntry> { it.rect.union(rect).area - it.rect.area }.thenBy { it.rect.area },
                    )
                }
            path.add(checkNotNull(best.child))
        }
        return path
    }

    // Moves part of node's entries into a new sibling node, which it returns.
    private fun split(
        node: RTreeNode,
        strategy: GKRTreeSplitStrategy,
    ): RTreeNode {
        val (first, second) =
            when (strategy) {
                GKRTreeSplitStrategy.HALVE -> halveSplit(node.entries)
                GKRTreeSplitStrategy.LINEAR ->
                    seededSplit(
                        node.entries,
                        linearSeeds(node.entries),
                        pickMostDecided = false,
                    )
                GKRTreeSplitStrategy.QUADRATIC ->
                    seededSplit(
                        node.entries,
                        quadraticSeeds(node.entries),
                        pickMostDecided = true,
                    )
                GKRTreeSplitStrategy.REDUCE_OVERLAP -> reduceOverlapSplit(node.entries)
            }
        node.entries.clear()
        node.entries.addAll(first)
        return RTreeNode(node.isLeaf).also { it.entries.addAll(second) }
    }

    private fun halveSplit(entries: List<RTreeEntry>): Pair<List<RTreeEntry>, List<RTreeEntry>> {
        val spreadX = entries.maxOf { it.rect.centerX } - entries.minOf { it.rect.centerX }
        val spreadY = entries.maxOf { it.rect.centerY } - entries.minOf { it.rect.centerY }
        val sorted =
            if (spreadX >= spreadY) entries.sortedBy { it.rect.centerX } else entries.sortedBy { it.rect.centerY }
        val half = sorted.size / 2
        return sorted.take(half) to sorted.drop(half)
    }

    private fun linearSeeds(entries: List<RTreeEntry>): Pair<Int, Int> {
        fun seedsAlong(
            low: (RTreeRect) -> Float,
            high: (RTreeRect) -> Float,
        ): Triple<Int, Int, Float> {
            val highestLow = entries.indices.maxBy { low(entries[it].rect) }
            val lowestHigh = entries.indices.filter { it != highestLow }.minBy { high(entries[it].rect) }
            val width = entries.maxOf { high(it.rect) } - entries.minOf { low(it.rect) }
            val separation = low(entries[highestLow].rect) - high(entries[lowestHigh].rect)
            return Triple(lowestHigh, highestLow, if (width > 0f) separation / width else 0f)
        }
        val alongX = seedsAlong({ it.min.x }, { it.max.x })
        val alongY = seedsAlong({ it.min.y }, { it.max.y })
        val best = if (alongX.third >= alongY.third) alongX else alongY
        return best.first to best.second
    }

    private fun quadraticSeeds(entries: List<RTreeEntry>): Pair<Int, Int> {
        var best = 0 to 1
        var worstWaste = Float.NEGATIVE_INFINITY
        for (i in entries.indices) {
            for (j in i + 1 until entries.size) {
                val waste = entries[i].rect.union(entries[j].rect).area - entries[i].rect.area - entries[j].rect.area
                if (waste > worstWaste) {
                    worstWaste = waste
                    best = i to j
                }
            }
        }
        return best
    }

    // Guttman's distribution: start a group from each seed, then hand out the rest one at a time to
    // the group that grows least — the next entry in order (linear) or the entry with the strongest
    // preference (quadratic) — until one group must take everything left to reach minimumFill.
    private fun seededSplit(
        entries: List<RTreeEntry>,
        seeds: Pair<Int, Int>,
        pickMostDecided: Boolean,
    ): Pair<List<RTreeEntry>, List<RTreeEntry>> {
        val first = mutableListOf(entries[seeds.first])
        val second = mutableListOf(entries[seeds.second])
        var firstRect = first[0].rect
        var secondRect = second[0].rect
        val remaining =
            entries.indices.filter { it != seeds.first && it != seeds.second }.map { entries[it] }.toMutableList()
        while (remaining.isNotEmpty()) {
            val forced =
                when (minimumFill) {
                    first.size + remaining.size -> first
                    second.size + remaining.size -> second
                    else -> null
                }
            if (forced != null) {
                forced.addAll(remaining)
                remaining.clear()
            } else {
                val next =
                    if (pickMostDecided) {
                        remaining.maxBy { abs(growth(firstRect, it.rect) - growth(secondRect, it.rect)) }
                    } else {
                        remaining.first()
                    }
                remaining.remove(next)
                val preference =
                    compareValues(growth(firstRect, next.rect), growth(secondRect, next.rect)).takeIf { it != 0 }
                        ?: compareValues(firstRect.area, secondRect.area).takeIf { it != 0 }
                        ?: compareValues(first.size, second.size)
                if (preference <= 0) {
                    first.add(next)
                    firstRect = firstRect.union(next.rect)
                } else {
                    second.add(next)
                    secondRect = secondRect.union(next.rect)
                }
            }
        }
        return first to second
    }

    // R*-tree split: pick the axis whose candidate cuts have the smallest total margin, then the cut
    // on that axis with the least overlap between the two halves (ties: least total area).
    private fun reduceOverlapSplit(entries: List<RTreeEntry>): Pair<List<RTreeEntry>, List<RTreeEntry>> {
        val cuts = minimumFill..(entries.size - minimumFill)

        fun sortings(axisX: Boolean): List<List<RTreeEntry>> =
            if (axisX) {
                listOf(entries.sortedBy { it.rect.min.x }, entries.sortedBy { it.rect.max.x })
            } else {
                listOf(entries.sortedBy { it.rect.min.y }, entries.sortedBy { it.rect.max.y })
            }

        fun marginSum(axisX: Boolean): Float =
            sortings(axisX).sumOf { sorted ->
                cuts.sumOf { cut -> (boundsOf(sorted.take(cut)).margin + boundsOf(sorted.drop(cut)).margin).toDouble() }
            }.toFloat()

        val axisX = marginSum(true) <= marginSum(false)
        return sortings(axisX)
            .flatMap { sorted -> cuts.map { cut -> sorted.take(cut) to sorted.drop(cut) } }
            .minWith(
                compareBy<Pair<List<RTreeEntry>, List<RTreeEntry>>> { (a, b) -> boundsOf(a).overlap(boundsOf(b)) }
                    .thenBy { (a, b) -> boundsOf(a).area + boundsOf(b).area },
            )
    }

    private fun overlapEnlargement(
        entry: RTreeEntry,
        rect: RTreeRect,
        siblings: List<RTreeEntry>,
    ): Float {
        val grown = entry.rect.union(rect)
        return siblings.filter {
            it !== entry
        }.sumOf { (grown.overlap(it.rect) - entry.rect.overlap(it.rect)).toDouble() }.toFloat()
    }

    private fun removeFrom(
        node: RTreeNode,
        element: ElementType,
        rect: RTreeRect,
    ): Boolean =
        if (node.isLeaf) {
            val index = node.entries.indexOfFirst { it.element == element }
            if (index >= 0) node.entries.removeAt(index)
            index >= 0
        } else {
            val found =
                node.entries.firstOrNull { entry ->
                    entry.rect.contains(rect) && removeFrom(checkNotNull(entry.child), element, rect)
                }
            found?.let { entry ->
                val child = checkNotNull(entry.child)
                if (child.entries.isEmpty()) node.entries.remove(entry) else entry.rect = child.bounds()
            }
            found != null
        }

    private fun collect(
        node: RTreeNode,
        query: RTreeRect,
        result: MutableList<ElementType>,
    ) {
        node.entries.forEach { entry ->
            if (node.isLeaf) {
                if (query.contains(entry.rect)) result.add(castRTreeElement(checkNotNull(entry.element)))
            } else if (entry.rect.intersects(query)) {
                collect(checkNotNull(entry.child), query, result)
            }
        }
    }
}

private fun growth(
    rect: RTreeRect,
    added: RTreeRect,
): Float = rect.union(added).area - rect.area

private fun boundsOf(entries: List<RTreeEntry>): RTreeRect = entries.map { it.rect }.reduce(RTreeRect::union)

private class RTreeNode(
    val isLeaf: Boolean,
) {
    val entries: MutableList<RTreeEntry> = mutableListOf()

    fun bounds(): RTreeRect = boundsOf(entries)
}

// A leaf entry carries an element; an internal entry carries a child node and that child's bounds.
private class RTreeEntry(
    var rect: RTreeRect,
    val element: Any? = null,
    val child: RTreeNode? = null,
)

private data class RTreeRect(
    val min: Vector2,
    val max: Vector2,
) {
    val area: Float get() = (max.x - min.x) * (max.y - min.y)
    val margin: Float get() = (max.x - min.x) + (max.y - min.y)
    val centerX: Float get() = (min.x + max.x) / 2f
    val centerY: Float get() = (min.y + max.y) / 2f

    fun union(other: RTreeRect): RTreeRect =
        RTreeRect(
            Vector2(minOf(min.x, other.min.x), minOf(min.y, other.min.y)),
            Vector2(maxOf(max.x, other.max.x), maxOf(max.y, other.max.y)),
        )

    fun contains(other: RTreeRect): Boolean =
        other.min.x >= min.x && other.min.y >= min.y && other.max.x <= max.x && other.max.y <= max.y

    fun intersects(other: RTreeRect): Boolean =
        min.x <= other.max.x && max.x >= other.min.x && min.y <= other.max.y && max.y >= other.min.y

    fun overlap(other: RTreeRect): Float {
        val width = minOf(max.x, other.max.x) - maxOf(min.x, other.min.x)
        val height = minOf(max.y, other.max.y) - maxOf(min.y, other.min.y)
        return if (width > 0f && height > 0f) width * height else 0f
    }
}

@Suppress("UNCHECKED_CAST")
private fun <ElementType : Any> castRTreeElement(element: Any): ElementType = element as ElementType
