package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

/**
 * A [GKGraphNode] with a 3D floating-point position. Matches GameplayKit's default: [cost] and
 * [estimatedCost] both equal the straight-line (Euclidean) distance between positions.
 */
public open class GKGraphNode3D(
    public var position: Vector3,
) : GKGraphNode() {
    override fun cost(node: GKGraphNode): Float =
        if (node is GKGraphNode3D) distance(position, node.position) else super.cost(node)

    override fun estimatedCost(node: GKGraphNode): Float = cost(node)
}

private fun distance(
    a: Vector3,
    b: Vector3,
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}
