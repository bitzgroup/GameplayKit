package jp.co.bitz.gameplaykit

/**
 * A [GKGraph] in which movement is constrained to an integer grid, mirroring GameplayKit's
 * `GKGridGraph`. The full `gridWidth x gridHeight` grid of [GKGridGraphNode] is created and
 * connected to its orthogonal (and, if [diagonalsAllowed], diagonal) neighbors up front.
 *
 * [nodeFactory] builds each of those nodes — Apple's `nodeClass:` argument, which builds them with
 * the class's `init(gridPosition:)`. Pass a subclass's constructor (`::MyNode`) to fill the grid
 * with it; lookups still return [GKGridGraphNode], so cast back. Nodes added later through
 * [connectNodeToAdjacentNodes] needn't be of that class, as on Apple (verified on iOS/tvOS
 * simulators).
 */
public class GKGridGraph(
    public val gridOrigin: Vector2Int,
    public val gridWidth: Int,
    public val gridHeight: Int,
    public val diagonalsAllowed: Boolean,
    nodeFactory: (gridPosition: Vector2Int) -> GKGridGraphNode = ::GKGridGraphNode,
) : GKGraph() {
    private val nodesByPosition: MutableMap<Vector2Int, GKGridGraphNode> = mutableMapOf()

    init {
        for (dx in 0 until gridWidth) {
            for (dy in 0 until gridHeight) {
                val position = Vector2Int(gridOrigin.x + dx, gridOrigin.y + dy)
                val node = nodeFactory(position)
                nodesByPosition[position] = node
                add(listOf(node))
            }
        }
        nodesByPosition.values.forEach { connectNodeToAdjacentNodes(it) }
    }

    /**
     * Returns the node at [position], or `null` if there is none — including for any position
     * outside `gridOrigin`/`gridWidth`/`gridHeight`, even one a node was connected at, as in Apple's.
     */
    public fun node(position: Vector2Int): GKGridGraphNode? =
        nodesByPosition[position]?.takeIf { isInsideGrid(position) }

    /**
     * Adds [node] to the graph (if it isn't already), registers it at its
     * [GKGridGraphNode.gridPosition] and links it to whichever of its (up to 8) neighboring grid
     * positions have a node — Apple's `connectToAdjacentNodes(node:)`. A node outside the grid joins
     * the graph and its neighbors, but [node] lookup doesn't find it, as in Apple's (verified on
     * iOS/tvOS simulators).
     */
    public fun connectNodeToAdjacentNodes(node: GKGridGraphNode) {
        if (nodesByPosition[node.gridPosition] !== node && node !in nodes) add(listOf(node))
        nodesByPosition[node.gridPosition] = node
        neighborOffsets().forEach { (dx, dy) ->
            val neighborPosition = Vector2Int(node.gridPosition.x + dx, node.gridPosition.y + dy)
            nodesByPosition[neighborPosition]?.let { neighbor ->
                if (neighbor !== node) node.addConnections(listOf(neighbor), bidirectional = true)
            }
        }
    }

    /**
     * Removes [nodes] like [GKGraph.remove], and also from position lookup — so [node] returns
     * `null` for their positions, as in Apple's.
     */
    override fun remove(nodes: List<GKGraphNode>) {
        super.remove(nodes)
        nodes.filterIsInstance<GKGridGraphNode>().forEach { node ->
            if (nodesByPosition[node.gridPosition] === node) nodesByPosition.remove(node.gridPosition)
        }
    }

    private fun isInsideGrid(position: Vector2Int): Boolean =
        position.x - gridOrigin.x in 0 until gridWidth && position.y - gridOrigin.y in 0 until gridHeight

    private fun neighborOffsets(): List<Pair<Int, Int>> {
        val orthogonal = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        val diagonal = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        return if (diagonalsAllowed) orthogonal + diagonal else orthogonal
    }
}
