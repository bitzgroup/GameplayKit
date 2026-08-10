package jp.co.bitz.gameplaykit

import kotlin.math.sqrt

/**
 * A 2D single-precision vector. Stands in for GameplayKit's `vector_float2` (a SIMD type) since
 * there is no SIMD vector type in the Kotlin/Android standard library.
 */
public data class Vector2(
    public val x: Float,
    public val y: Float,
) {
    public operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)

    public operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)

    public operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)

    /** The dot product of this vector and [other]. */
    public infix fun dot(other: Vector2): Float = x * other.x + y * other.y

    /** The Euclidean length (magnitude) of this vector. */
    public fun length(): Float = sqrt(x * x + y * y)

    /**
     * Returns a unit-length vector in the same direction, or this vector unchanged if it is zero
     * (there is no well-defined direction to normalize to).
     */
    public fun normalized(): Vector2 {
        val len = length()
        return if (len > 0f) Vector2(x / len, y / len) else this
    }
}
