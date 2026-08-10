package jp.co.bitz.gameplaykit

import kotlin.math.floor

/**
 * Slices a finite, two-dimensional rectangle from a [GKNoise] object's infinite noise field,
 * mirroring GameplayKit's `GKNoiseMap`. [size]/[origin] describe the sampled rectangle in the
 * noise's own coordinate space; [sampleCount] is how many discrete grid points to divide it into.
 *
 * Deviation from GameplayKit: [seamless] here wraps grid coordinates modulo [sampleCount] so the
 * last row/column resamples the same positions as the first (a simple, tileable approximation);
 * GameplayKit doesn't document its own seamless-blending algorithm, so this is
 * contract-conformant (produces a tileable map), not bit-identical.
 */
public class GKNoiseMap(
    private val noise: GKNoise,
    public val size: Vector2 = Vector2(1f, 1f),
    public val origin: Vector2 = Vector2(0f, 0f),
    public val sampleCount: Vector2Int = Vector2Int(1, 1),
    public val seamless: Boolean = false,
) {
    /**
     * Creates a 1x1 map, sampling a single point of a zero-valued [GKNoise], mirroring
     * GameplayKit's parameterless constructor.
     */
    public constructor() : this(GKNoise())

    private val overrides: MutableMap<Vector2Int, Float> = mutableMapOf()

    /** The noise value at grid coordinate [at], or an override set via [setValue] if one exists there. */
    public fun value(at: Vector2Int): Float {
        val coordinate = if (seamless) wrap(at) else at
        return overrides[coordinate] ?: sample(coordinate)
    }

    /** Overrides the value at grid coordinate [at], replacing whatever [noise] would otherwise sample there. */
    public fun setValue(
        value: Float,
        at: Vector2Int,
    ) {
        overrides[if (seamless) wrap(at) else at] = value
    }

    /**
     * Bilinearly interpolates between the four grid points surrounding [at], in the map's
     * continuous coordinate space.
     */
    public fun interpolatedValue(at: Vector2): Float {
        val gx = gridCoordinate(at.x, origin.x, size.x, sampleCount.x)
        val gy = gridCoordinate(at.y, origin.y, size.y, sampleCount.y)
        val x0 = floor(gx).toInt().coerceIn(0, maxOf(sampleCount.x - 1, 0))
        val y0 = floor(gy).toInt().coerceIn(0, maxOf(sampleCount.y - 1, 0))
        val x1 = (x0 + 1).coerceAtMost(maxOf(sampleCount.x - 1, 0))
        val y1 = (y0 + 1).coerceAtMost(maxOf(sampleCount.y - 1, 0))
        val tx = (gx - x0).coerceIn(0f, 1f)
        val ty = (gy - y0).coerceIn(0f, 1f)

        val top = lerp(value(Vector2Int(x0, y0)), value(Vector2Int(x1, y0)), tx)
        val bottom = lerp(value(Vector2Int(x0, y1)), value(Vector2Int(x1, y1)), tx)
        return lerp(top, bottom, ty)
    }

    private fun gridCoordinate(
        position: Float,
        origin: Float,
        extent: Float,
        count: Int,
    ): Float = if (extent == 0f || count <= 1) 0f else ((position - origin) / extent) * (count - 1)

    private fun lerp(
        a: Float,
        b: Float,
        t: Float,
    ): Float = a + (b - a) * t

    // `coordinate` is expected to already be wrapped/clamped by the caller (value/setValue).
    private fun sample(coordinate: Vector2Int): Float {
        val fx = if (sampleCount.x > 1) coordinate.x.toFloat() / (sampleCount.x - 1) else 0f
        val fy = if (sampleCount.y > 1) coordinate.y.toFloat() / (sampleCount.y - 1) else 0f
        return noise.value(at = Vector2(origin.x + fx * size.x, origin.y + fy * size.y))
    }

    private fun wrap(coordinate: Vector2Int): Vector2Int {
        val wrappedX = if (sampleCount.x > 0) coordinate.x.mod(sampleCount.x) else coordinate.x
        val wrappedY = if (sampleCount.y > 0) coordinate.y.mod(sampleCount.y) else coordinate.y
        return Vector2Int(wrappedX, wrappedY)
    }
}
