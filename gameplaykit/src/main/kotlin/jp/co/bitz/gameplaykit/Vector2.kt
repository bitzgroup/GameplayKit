package jp.co.bitz.gameplaykit

// A 2D single-precision vector. Stands in for GameplayKit's `vector_float2` (a SIMD type) since
// there is no SIMD vector type in the Kotlin/Android standard library.
public data class Vector2(
    public val x: Float,
    public val y: Float,
)
