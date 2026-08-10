package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Uses a [GKNoiseSource] to procedurally generate an infinite three-dimensional noise field,
 * mirroring GameplayKit's `GKNoise`. Each operation below ([add], [multiply], [displace], ...)
 * mutates this instance in place by wrapping its current sampling function, the same "builder via
 * mutation" shape GameplayKit's own `GKNoise` API has.
 *
 * Deviation from GameplayKit: positions are this library's existing single-precision [Vector3]
 * (GameplayKit's `GKNoise` actually samples a double-precision `vector_double3`). Introducing a
 * parallel double-precision vector type solely for this one API wasn't judged worth the surface
 * duplication; `Float` precision is adequate for typical noise-map/terrain sampling. Not
 * implemented: the `gradientColors`-based constructor/property (tied to `UIColor`/`NSColor`, no
 * Android equivalent wired into this library) and `remapValuesToCurve`/`remapValuesToTerraces`
 * (texture/curve-editing conveniences, rendering-adjacent rather than algorithmic core, per this
 * phase's "algorithmic core only" scope).
 */
public class GKNoise(noiseSource: GKNoiseSource) {
    /** Creates a noise field that samples zero everywhere, mirroring GameplayKit's parameterless `GKNoise()`. */
    public constructor() : this(GKConstantNoiseSource(0.0))

    private var sampler: (Vector3) -> Double = { p -> noiseSource.sample(p) }

    /** Samples the noise field on the X/Z plane (Y = 0) — the conventional ground plane for terrain-style 2D use. */
    public fun value(at: Vector2): Float = sampler(Vector3(at.x, 0f, at.y)).toFloat()

    /** Adds [noise]'s value to this field's value at every point. */
    public fun add(noise: GKNoise) {
        combine(noise) { a, b -> a + b }
    }

    /** Multiplies this field's value by [noise]'s value at every point. */
    public fun multiply(noise: GKNoise) {
        combine(noise) { a, b -> a * b }
    }

    /** Replaces this field's value at every point with the greater of it and [noise]'s value there. */
    public fun getMaximum(noise: GKNoise) {
        combine(noise) { a, b -> maxOf(a, b) }
    }

    /** Replaces this field's value at every point with the lesser of it and [noise]'s value there. */
    public fun getMinimum(noise: GKNoise) {
        combine(noise) { a, b -> minOf(a, b) }
    }

    /** Raises this field's value at every point to the power of [exponent]. */
    public fun raiseToPower(exponent: Double) {
        val old = sampler
        sampler = { p -> old(p).pow(exponent) }
    }

    /** Raises this field's value at every point to the power of [noise]'s value there. */
    public fun raiseToPower(noise: GKNoise) {
        combine(noise) { a, b -> a.pow(b) }
    }

    /** Clamps this field's value at every point to `[min, max]`. */
    public fun clamp(
        min: Double,
        max: Double,
    ) {
        val old = sampler
        sampler = { p -> old(p).coerceIn(min, max) }
    }

    /** Replaces this field's value at every point with its absolute value. */
    public fun applyAbsoluteValue() {
        val old = sampler
        sampler = { p -> abs(old(p)) }
    }

    /** Negates this field's value at every point. */
    public fun invert() {
        val old = sampler
        sampler = { p -> -old(p) }
    }

    /**
     * Perturbs the sampling position along each axis by an independent Perlin field before
     * delegating to the current noise, mirroring GameplayKit's `applyTurbulence`.
     */
    public fun applyTurbulence(
        frequency: Double,
        power: Double,
        roughness: Int,
        seed: Int,
    ) {
        val perturbX = GKPerlinNoiseSource(frequency, roughness, PERSISTENCE, LACUNARITY, seed)
        val perturbY = GKPerlinNoiseSource(frequency, roughness, PERSISTENCE, LACUNARITY, seed + 1)
        val perturbZ = GKPerlinNoiseSource(frequency, roughness, PERSISTENCE, LACUNARITY, seed + 2)
        val old = sampler
        sampler = { p ->
            old(
                Vector3(
                    p.x + (perturbX.sample(p) * power).toFloat(),
                    p.y + (perturbY.sample(p) * power).toFloat(),
                    p.z + (perturbZ.sample(p) * power).toFloat(),
                ),
            )
        }
    }

    /**
     * Offsets each sampled position by an independent noise field per axis, mirroring
     * GameplayKit's `displace(xDisplacementNoise:yDisplacementNoise:zDisplacementNoise:)`.
     */
    public fun displace(
        xNoise: GKNoise,
        yNoise: GKNoise,
        zNoise: GKNoise,
    ) {
        val old = sampler
        sampler = { p ->
            old(
                Vector3(
                    p.x + xNoise.sampler(p).toFloat(),
                    p.y + yNoise.sampler(p).toFloat(),
                    p.z + zNoise.sampler(p).toFloat(),
                ),
            )
        }
    }

    /** Translates the noise field by [by]. */
    public fun move(by: Vector3) {
        val old = sampler
        sampler = { p -> old(p - by) }
    }

    /** Scales the noise field by [by] along each axis (a component of `1` leaves that axis unscaled). */
    public fun scale(by: Vector3) {
        val old = sampler
        sampler = { p -> old(Vector3(safeDivide(p.x, by.x), safeDivide(p.y, by.y), safeDivide(p.z, by.z))) }
    }

    /**
     * Rotates the sample position by Euler angles (radians, applied X then Y then Z) before
     * delegating to the current noise. GameplayKit doesn't document its own rotation order, so
     * this is contract-conformant (a valid 3D rotation), not necessarily bit-identical.
     */
    public fun rotate(by: Vector3) {
        val old = sampler
        sampler = { p -> old(rotateVector(p, by)) }
    }

    private fun combine(
        noise: GKNoise,
        operation: (Double, Double) -> Double,
    ) {
        val old = sampler
        val other = noise.sampler
        sampler = { p -> operation(old(p), other(p)) }
    }

    private companion object {
        private const val PERSISTENCE = 0.5
        private const val LACUNARITY = 2.0
    }
}

private fun safeDivide(
    value: Float,
    divisor: Float,
): Float = if (divisor == 0f) value else value / divisor

private fun rotateVector(
    p: Vector3,
    radians: Vector3,
): Vector3 {
    val afterX = rotateAroundX(p, radians.x)
    val afterY = rotateAroundY(afterX, radians.y)
    return rotateAroundZ(afterY, radians.z)
}

private fun rotateAroundX(
    p: Vector3,
    radians: Float,
): Vector3 {
    val c = cos(radians.toDouble()).toFloat()
    val s = sin(radians.toDouble()).toFloat()
    return Vector3(p.x, p.y * c - p.z * s, p.y * s + p.z * c)
}

private fun rotateAroundY(
    p: Vector3,
    radians: Float,
): Vector3 {
    val c = cos(radians.toDouble()).toFloat()
    val s = sin(radians.toDouble()).toFloat()
    return Vector3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c)
}

private fun rotateAroundZ(
    p: Vector3,
    radians: Float,
): Vector3 {
    val c = cos(radians.toDouble()).toFloat()
    val s = sin(radians.toDouble()).toFloat()
    return Vector3(p.x * c - p.y * s, p.x * s + p.y * c, p.z)
}
