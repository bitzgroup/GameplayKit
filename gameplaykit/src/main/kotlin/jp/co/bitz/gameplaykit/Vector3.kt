package jp.co.bitz.gameplaykit

// A 3D single-precision vector. Stands in for GameplayKit's `vector_float3` (a SIMD type) since
// there is no SIMD vector type in the Kotlin/Android standard library.
public data class Vector3(
    public val x: Float,
    public val y: Float,
    public val z: Float,
)
