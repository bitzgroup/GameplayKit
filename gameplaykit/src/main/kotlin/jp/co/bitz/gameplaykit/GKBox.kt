package jp.co.bitz.gameplaykit

/** An axis-aligned rectangular 3D region, mirroring GameplayKit's `GKBox`. */
public data class GKBox(
    public val min: Vector3,
    public val max: Vector3,
) {
    internal fun contains(other: GKBox): Boolean =
        other.min.x >= min.x && other.min.y >= min.y && other.min.z >= min.z &&
            other.max.x <= max.x && other.max.y <= max.y && other.max.z <= max.z

    internal fun intersects(other: GKBox): Boolean =
        min.x <= other.max.x && max.x >= other.min.x &&
            min.y <= other.max.y && max.y >= other.min.y &&
            min.z <= other.max.z && max.z >= other.min.z
}
