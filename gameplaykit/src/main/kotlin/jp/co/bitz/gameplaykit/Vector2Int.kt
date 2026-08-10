package jp.co.bitz.gameplaykit

/**
 * A 2D integer vector. Stands in for GameplayKit's `vector_int2` (a SIMD type), used for grid
 * coordinates such as [GKGridGraphNode.gridPosition].
 */
public data class Vector2Int(
    public val x: Int,
    public val y: Int,
)
