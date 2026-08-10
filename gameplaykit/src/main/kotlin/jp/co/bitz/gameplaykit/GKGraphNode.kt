package jp.co.bitz.gameplaykit

/**
 * The base class for nodes in a graph, mirroring GameplayKit's `GKGraphNode`. Connections are
 * directed unless made bidirectional; pathfinding ([pathFrom]/[pathTo], and [GKGraph.findPath])
 * traverses each node's own [connectedNodes] directly, independent of which [GKGraph] (if any)
 * it's registered with.
 */
public open class GKGraphNode {
    private val mutableConnectedNodes: LinkedHashSet<GKGraphNode> = linkedSetOf()

    /** The nodes this node has an outgoing connection to. */
    public val connectedNodes: List<GKGraphNode>
        get() = mutableConnectedNodes.toList()

    /** Connects this node to each of [nodes]; if [bidirectional], each also connects back to this node. */
    public open fun addConnections(
        nodes: List<GKGraphNode>,
        bidirectional: Boolean,
    ) {
        nodes.forEach { node ->
            mutableConnectedNodes.add(node)
            if (bidirectional) node.mutableConnectedNodes.add(this)
        }
    }

    /** Removes this node's connection to each of [nodes]; if [bidirectional], also removes the reverse connection. */
    public open fun removeConnections(
        nodes: List<GKGraphNode>,
        bidirectional: Boolean,
    ) {
        nodes.forEach { node ->
            mutableConnectedNodes.remove(node)
            if (bidirectional) node.mutableConnectedNodes.remove(this)
        }
    }

    /**
     * The cost of moving from this node to [node], used by pathfinding. Base implementation
     * reports a uniform unit cost, so an unsubclassed graph still supports pathfinding
     * (degrading to breadth-first search). [GKGraphNode2D], [GKGraphNode3D], and
     * [GKGridGraphNode] override this with real distance-based values.
     */
    public open fun cost(node: GKGraphNode): Float = 1f

    /**
     * A pathfinding heuristic: an estimate of the remaining cost to reach [node], used to guide
     * A* search. Base implementation reports zero, degrading to breadth-first search.
     */
    public open fun estimatedCost(node: GKGraphNode): Float = 0f

    /**
     * The shortest path from [node] to this node, as an ordered list of nodes (empty if no path
     * exists). Named `pathFrom`/`pathTo` rather than two overloads of GameplayKit's
     * `findPath(from:)`/`findPath(to:)`: Kotlin has no argument-label-based overload resolution,
     * so those two Swift signatures (which differ only by label) would collide as plain Kotlin
     * overloads.
     */
    public fun pathFrom(node: GKGraphNode): List<GKGraphNode> = aStarPath(node, this)

    /** The shortest path from this node to [node], as an ordered list of nodes (empty if no path exists). */
    public fun pathTo(node: GKGraphNode): List<GKGraphNode> = aStarPath(this, node)
}
