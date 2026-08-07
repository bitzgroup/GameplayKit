package jp.co.bitz.gameplaykit

import java.util.PriorityQueue

// A* search over a GKGraphNode network, shared by GKGraph.findPath and GKGraphNode.pathFrom/pathTo.
// Traverses `connectedNodes` directly, so it works regardless of which (if any) GKGraph the nodes are
// registered with — matching GameplayKit, where connectivity (not GKGraph membership) drives pathing.
internal fun aStarPath(
    start: GKGraphNode,
    goal: GKGraphNode,
): List<GKGraphNode> {
    if (start === goal) return listOf(start)

    val gScore = mutableMapOf(start to 0f)
    val cameFrom = mutableMapOf<GKGraphNode, GKGraphNode>()
    val closed = mutableSetOf<GKGraphNode>()
    val open = PriorityQueue<Pair<GKGraphNode, Float>>(compareBy { it.second })
    open.add(start to start.estimatedCost(goal))

    while (open.isNotEmpty()) {
        // open.isNotEmpty() above guarantees poll() won't return null here.
        val current = checkNotNull(open.poll()).first
        if (current === goal) break
        if (closed.add(current)) {
            relaxNeighbors(current, goal, gScore, cameFrom, open)
        }
    }

    return if (goal in cameFrom) buildPath(cameFrom, goal) else emptyList()
}

private fun relaxNeighbors(
    current: GKGraphNode,
    goal: GKGraphNode,
    gScore: MutableMap<GKGraphNode, Float>,
    cameFrom: MutableMap<GKGraphNode, GKGraphNode>,
    open: PriorityQueue<Pair<GKGraphNode, Float>>,
) {
    current.connectedNodes.forEach { neighbor ->
        val tentativeGScore = gScore.getValue(current) + current.cost(neighbor)
        if (tentativeGScore < (gScore[neighbor] ?: Float.MAX_VALUE)) {
            cameFrom[neighbor] = current
            gScore[neighbor] = tentativeGScore
            open.add(neighbor to tentativeGScore + neighbor.estimatedCost(goal))
        }
    }
}

private fun buildPath(
    cameFrom: Map<GKGraphNode, GKGraphNode>,
    goal: GKGraphNode,
): List<GKGraphNode> {
    val path = mutableListOf(goal)
    var current = goal
    while (true) {
        val previous = cameFrom[current] ?: break
        path.add(previous)
        current = previous
    }
    return path.asReversed()
}
