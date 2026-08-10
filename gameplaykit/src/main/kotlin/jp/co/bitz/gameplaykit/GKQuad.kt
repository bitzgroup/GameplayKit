package jp.co.bitz.gameplaykit

/** An axis-aligned rectangular 2D region, mirroring GameplayKit's `GKQuad`. */
public data class GKQuad(
    public val min: Vector2,
    public val max: Vector2,
) {
    internal fun contains(other: GKQuad): Boolean =
        other.min.x >= min.x && other.min.y >= min.y && other.max.x <= max.x && other.max.y <= max.y

    internal fun intersects(other: GKQuad): Boolean =
        min.x <= other.max.x && max.x >= other.min.x && min.y <= other.max.y && max.y >= other.min.y
}
