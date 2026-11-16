package jp.co.bitz.gameplaykit

/**
 * A mathematical graph used for navigability and pathfinding, mirroring GameplayKit's `GKGraph`.
 * [nodes] is a bookkeeping registry; actual pathfinding traverses each node's own connections
 * (see [GKGraphNode.connectedNodes]) regardless of graph membership.
 */
public open class GKGraph(nodes: List<GKGraphNode> = emptyList()) {
    private val mutableNodes: MutableList<GKGraphNode> = nodes.toMutableList()

    /** The nodes registered with this graph. */
    public val nodes: List<GKGraphNode>
        get() = mutableNodes.toList()

    /** Registers [nodes] with this graph. Does not create any connections. */
    public open fun add(nodes: List<GKGraphNode>) {
        mutableNodes.addAll(nodes)
    }

    /**
     * Unregisters [nodes] from this graph and cuts every connection to or from them, in both
     * directions, as Apple's does (verified on iOS/tvOS simulators) — so pathfinding no longer
     * routes through a removed node.
     */
    public open fun remove(nodes: List<GKGraphNode>) {
        val removed = nodes.toSet()
        mutableNodes.removeAll(removed)
        removed.forEach { node -> node.removeConnections(node.connectedNodes, bidirectional = true) }
        mutableNodes.forEach {
                node ->
            node.removeConnections(node.connectedNodes.filter { it in removed }, bidirectional = false)
        }
    }

    /**
     * Connects [node] to whichever other node in the graph it reports the lowest [GKGraphNode.cost]
     * to — handy for wiring a freshly added node into an existing graph without picking a neighbor
     * by hand.
     */
    public open fun connectToLowestCostNode(
        node: GKGraphNode,
        bidirectional: Boolean,
    ) {
        val lowestCostNode = mutableNodes.filter { it !== node }.minByOrNull { node.cost(it) } ?: return
        node.addConnections(listOf(lowestCostNode), bidirectional)
    }

    /** The shortest path from [from] to [to], as an ordered list of nodes (empty if no path exists). */
    public fun findPath(
        from: GKGraphNode,
        to: GKGraphNode,
    ): List<GKGraphNode> = aStarPath(from, to)
}
