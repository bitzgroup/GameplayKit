package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

/**
 * A [GKGraphNode] with a 2D floating-point position. Matches GameplayKit's default: [cost] and
 * [estimatedCost] both equal the straight-line (Euclidean) distance between positions.
 */
public open class GKGraphNode2D(
    public var position: Vector2,
) : GKGraphNode() {
    override fun cost(node: GKGraphNode): Float =
        if (node is GKGraphNode2D) distance(position, node.position) else super.cost(node)

    override fun estimatedCost(node: GKGraphNode): Float = cost(node)
}

private fun distance(
    a: Vector2,
    b: Vector2,
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
