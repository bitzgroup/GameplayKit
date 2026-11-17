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
 * Android equivalent wired into this library).
 *
 * As on GameplayKit (verified on iOS/tvOS simulators), [value] clamps what it returns to
 * `-1..1`, while the operations themselves work on unclamped values: a constant of `3` plus a
 * constant of `-2.5` samples as `0.5`, not `0`.
 */
public class GKNoise(noiseSource: GKNoiseSource) {
    /** Creates a noise field that samples zero everywhere, mirroring GameplayKit's parameterless `GKNoise()`. */
    public constructor() : this(GKConstantNoiseSource(0.0))

    private var sampler: (Vector3) -> Double = { p -> noiseSource.sample(p) }

    /**
     * Selects one of [componentNoises] at each point by [selectionNoise]'s value there, mirroring
     * GameplayKit's `init(componentNoises:selectionNoise:componentBoundaries:boundaryBlendDistances:)`
     * (and, with the defaults, `init(componentNoises:selectionNoise:)`).
     *
     * [componentBoundaries] (ascending, one fewer than the components) split the selection axis
     * into one band per component; `null` spaces them evenly over `-1..1`. A selection value on a
     * boundary picks the upper component. Matching GameplayKit (verified on iOS/tvOS simulators),
     * the chosen component's `-1..1` range is squeezed linearly into its band's span of `-1..1`
     * — with two components and the default boundary at `0`, the first one's values land in
     * `-1..0` and the second's in `0..1` — rather than passed through unchanged.
     * [boundaryBlendDistances] are accepted for API parity but, as on GameplayKit, have no effect.
     */
    public constructor(
        componentNoises: List<GKNoise>,
        selectionNoise: GKNoise,
        componentBoundaries: List<Double>? = null,
        boundaryBlendDistances: List<Double>? = null,
    ) : this(GKConstantNoiseSource(0.0)) {
        require(componentNoises.isNotEmpty()) { "componentNoises must not be empty" }
        val count = componentNoises.size
        val boundaries = componentBoundaries ?: List(count - 1) { i -> -1.0 + 2.0 * (i + 1) / count }
        require(boundaries.size == count - 1) {
            "componentBoundaries needs ${count - 1} values for $count components, got ${boundaries.size}"
        }
        require(boundaryBlendDistances == null || boundaryBlendDistances.size == count - 1) {
            "boundaryBlendDistances needs ${count - 1} values for $count components"
        }
        val bandEdges = listOf(-1.0) + boundaries + listOf(1.0)
        val components = componentNoises.map { it.sampler }
        val selection = selectionNoise.sampler
        sampler = { p ->
            if (count == 1) {
                components[0](p)
            } else {
                val s = selection(p)
                val band = boundaries.count { s >= it }
                val low = bandEdges[band]
                val high = bandEdges[band + 1]
                low + (components[band](p) + 1.0) / 2.0 * (high - low)
            }
        }
    }

    /**
     * Samples the noise field on the X/Z plane (Y = 0) — the conventional ground plane for
     * terrain-style 2D use — clamped to `-1..1`, as GameplayKit's `value(atPosition:)` is.
     */
    public fun value(at: Vector2): Float = sampler(Vector3(at.x, 0f, at.y)).coerceIn(-1.0, 1.0).toFloat()

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

    /**
     * Maps this field's value through a smooth curve through [controlPoints] (input → output),
     * mirroring GameplayKit's `remapValues(toCurveWithControlPoints:)`: cubic interpolation between
     * neighboring points, constant beyond the first and last. Needs at least two points.
     *
     * GameplayKit's own method can't be used to check against — it raises an exception for any
     * dictionary on current iOS/tvOS simulators — so this follows the libnoise curve module
     * GameplayKit's noise API is modeled on.
     */
    public fun remapValuesToCurve(controlPoints: Map<Double, Double>) {
        require(controlPoints.size >= 2) { "a curve needs at least two control points" }
        val points = controlPoints.entries.sortedBy { it.key }.map { it.key to it.value }
        val old = sampler
        sampler = { p -> curveValue(points, old(p)) }
    }

    /**
     * Maps this field's value onto terraces between [peaks], mirroring GameplayKit's
     * `remapValues(toTerracesWithPeaks:terracesInverted:)`: the value stays near the lower peak of
     * each interval and rises steeply into the upper one (or, with [terracesInverted], the reverse),
     * and is constant beyond the first and last peak. Needs at least two peaks.
     *
     * Follows the libnoise terrace module GameplayKit's noise API is modeled on, not GameplayKit's
     * current output, which doesn't trace terraces at all (recorded on iOS/tvOS simulators: not
     * monotonic, and `terracesInverted` often ignored) — see `docs/API_COMPATIBILITY.md`.
     */
    public fun remapValuesToTerraces(
        peaks: List<Double>,
        terracesInverted: Boolean,
    ) {
        val sorted = peaks.distinct().sorted()
        require(sorted.size >= 2) { "terraces need at least two distinct peaks" }
        val old = sampler
        sampler = { p -> terraceValue(sorted, terracesInverted, old(p)) }
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

/** libnoise's `Curve::GetValue`: cubic interpolation over the four points around [x]. */
private fun curveValue(
    points: List<Pair<Double, Double>>,
    x: Double,
): Double {
    val last = points.size - 1
    val upper = points.indexOfFirst { x < it.first }.let { if (it < 0) points.size else it }
    val i1 = (upper - 1).coerceIn(0, last)
    val i2 = upper.coerceIn(0, last)
    if (i1 == i2) return points[i1].second
    val i0 = (upper - 2).coerceIn(0, last)
    val i3 = (upper + 1).coerceIn(0, last)
    val alpha = (x - points[i1].first) / (points[i2].first - points[i1].first)
    return cubicInterpolate(points[i0].second, points[i1].second, points[i2].second, points[i3].second, alpha)
}

private fun cubicInterpolate(
    n0: Double,
    n1: Double,
    n2: Double,
    n3: Double,
    a: Double,
): Double {
    val p = (n3 - n2) - (n0 - n1)
    val q = (n0 - n1) - p
    val r = n2 - n0
    return p * a * a * a + q * a * a + r * a + n1
}

/** libnoise's `Terrace::GetValue`. */
private fun terraceValue(
    peaks: List<Double>,
    inverted: Boolean,
    x: Double,
): Double {
    val last = peaks.size - 1
    val upper = peaks.indexOfFirst { x < it }.let { if (it < 0) peaks.size else it }
    val i0 = (upper - 1).coerceIn(0, last)
    val i1 = upper.coerceIn(0, last)
    if (i0 == i1) return peaks[i1]
    var low = peaks[i0]
    var high = peaks[i1]
    var alpha = (x - low) / (high - low)
    if (inverted) {
        alpha = 1.0 - alpha
        low = high.also { high = low }
    }
    alpha *= alpha
    return low + (high - low) * alpha
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
