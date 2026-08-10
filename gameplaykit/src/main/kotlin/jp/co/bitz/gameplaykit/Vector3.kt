package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

/**
 * A 3D single-precision vector. Stands in for GameplayKit's `vector_float3` (a SIMD type) since
 * there is no SIMD vector type in the Kotlin/Android standard library.
 */
public data class Vector3(
    public val x: Float,
    public val y: Float,
    public val z: Float,
) {
    public operator fun plus(other: Vector3): Vector3 = Vector3(x + other.x, y + other.y, z + other.z)

    public operator fun minus(other: Vector3): Vector3 = Vector3(x - other.x, y - other.y, z - other.z)

    public operator fun times(scalar: Float): Vector3 = Vector3(x * scalar, y * scalar, z * scalar)

    /** The dot product of this vector and [other]. */
    public infix fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    /** The Euclidean length (magnitude) of this vector. */
    public fun length(): Float = sqrt(x * x + y * y + z * z)

    /**
     * Returns a unit-length vector in the same direction, or this vector unchanged if it is zero
     * (there is no well-defined direction to normalize to).
     */
    public fun normalized(): Vector3 {
        val len = length()
        return if (len > 0f) Vector3(x / len, y / len, z / len) else this
    }
}
