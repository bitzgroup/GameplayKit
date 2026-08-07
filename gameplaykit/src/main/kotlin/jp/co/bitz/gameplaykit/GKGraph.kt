package jp.co.bitz.gameplaykit

// A mathematical graph used for navigability and pathfinding, mirroring GameplayKit's GKGraph.
// `nodes` is a bookkeeping registry; actual pathfinding traverses each node's own connections (see
// GKGraphNode.connectedNodes) regardless of graph membership.
public open class GKGraph(nodes: List<GKGraphNode> = emptyList()) {
    private val mutableNodes: MutableList<GKGraphNode> = nodes.toMutableList()

    public val nodes: List<GKGraphNode>
        get() = mutableNodes.toList()

    public open fun add(nodes: List<GKGraphNode>) {
        mutableNodes.addAll(nodes)
    }

    public open fun remove(nodes: List<GKGraphNode>) {
        mutableNodes.removeAll(nodes.toSet())
    }

    // Connects `node` to whichever other node in the graph it reports the lowest cost() to — handy
    // for wiring a freshly added node into an existing graph without picking a neighbor by hand.
    public open fun connectToLowestCostNode(
        node: GKGraphNode,
        bidirectional: Boolean,
    ) {
        val lowestCostNode = mutableNodes.filter { it !== node }.minByOrNull { node.cost(it) } ?: return
        node.addConnections(listOf(lowestCostNode), bidirectional)
    }

    public fun findPath(
        from: GKGraphNode,
        to: GKGraphNode,
    ): List<GKGraphNode> = aStarPath(from, to)
}
