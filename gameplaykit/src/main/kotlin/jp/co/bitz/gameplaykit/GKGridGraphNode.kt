package jp.co.bitz.gameplaykit

import kotlin.math.abs

/**
 * A [GKGraphNode] with a 2D integer grid position, mirroring GameplayKit's `GKGridGraphNode`.
 * [cost]/[estimatedCost] use Chebyshev (chessboard) distance, so an orthogonal step and a diagonal
 * step both cost 1 — matching [GKGridGraph]'s uniform per-step movement model.
 */
public open class GKGridGraphNode(
    public val gridPosition: Vector2Int,
) : GKGraphNode() {
    override fun cost(node: GKGraphNode): Float =
        if (node is GKGridGraphNode) {
            maxOf(abs(gridPosition.x - node.gridPosition.x), abs(gridPosition.y - node.gridPosition.y)).toFloat()
        } else {
            super.cost(node)
        }

    override fun estimatedCost(node: GKGraphNode): Float = cost(node)
}
