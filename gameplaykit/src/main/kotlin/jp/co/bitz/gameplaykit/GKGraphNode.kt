package jp.co.bitz.gameplaykit

// The base class for nodes in a graph, mirroring GameplayKit's GKGraphNode. Connections are directed
// unless made bidirectional; pathfinding (`pathFrom`/`pathTo`, and GKGraph.findPath) traverses each
// node's own `connectedNodes` directly, independent of which GKGraph (if any) it's registered with.
public open class GKGraphNode {
    private val mutableConnectedNodes: LinkedHashSet<GKGraphNode> = linkedSetOf()

    public val connectedNodes: List<GKGraphNode>
        get() = mutableConnectedNodes.toList()

    public open fun addConnections(
        nodes: List<GKGraphNode>,
        bidirectional: Boolean,
    ) {
        nodes.forEach { node ->
            mutableConnectedNodes.add(node)
            if (bidirectional) node.mutableConnectedNodes.add(this)
        }
    }

    public open fun removeConnections(
        nodes: List<GKGraphNode>,
        bidirectional: Boolean,
    ) {
        nodes.forEach { node ->
            mutableConnectedNodes.remove(node)
            if (bidirectional) node.mutableConnectedNodes.remove(this)
        }
    }

    // Base implementation reports a uniform unit cost and a zero heuristic, so an unsubclassed graph
    // still supports pathfinding (degrading to breadth-first search). GKGraphNode2D, GKGraphNode3D,
    // and GKGridGraphNode override both with real distance-based values.
    public open fun cost(node: GKGraphNode): Float = 1f

    public open fun estimatedCost(node: GKGraphNode): Float = 0f

    // Named `pathFrom`/`pathTo` rather than two overloads of GameplayKit's `findPath(from:)` /
    // `findPath(to:)`: Kotlin has no argument-label-based overload resolution, so those two Swift
    // signatures (which differ only by label) would collide as plain Kotlin overloads.
    public fun pathFrom(node: GKGraphNode): List<GKGraphNode> = aStarPath(node, this)

    public fun pathTo(node: GKGraphNode): List<GKGraphNode> = aStarPath(this, node)
}
