package jp.co.bitz.gameplaykit

/**
 * A [GKNoiseSource] whose output varies smoothly and continuously, mirroring GameplayKit's
 * `GKCoherentNoiseSource`. Base for [GKPerlinNoiseSource]/[GKRidgedNoiseSource]/
 * [GKBillowNoiseSource], each of which sums [octaveCount] octaves of an underlying Perlin noise
 * field — starting at [frequency] and multiplying by [lacunarity] each octave — reproducibly
 * seeded by [seed].
 */
public abstract class GKCoherentNoiseSource(
    /** How many noise cycles fit per unit of sample-space distance in the first octave. */
    public var frequency: Double,
    /** How many summed octaves make up this source's output; higher adds finer detail. */
    public var octaveCount: Int,
    /** How much [frequency] multiplies by from one octave to the next. */
    public var lacunarity: Double,
    /** Seeds the underlying Perlin permutation table, making the field reproducible. */
    public var seed: Int,
) : GKNoiseSource() {
    private var cachedSeed: Int? = null
    private var cachedPerlin: PerlinNoise? = null

    // Rebuilds the underlying permutation table only when `seed` has actually changed since the
    // last sample, so mutating `seed` after construction takes effect on the next sample.
    internal fun currentPerlin(): PerlinNoise {
        val existing = cachedPerlin
        return if (cachedSeed == seed && existing != null) {
            existing
        } else {
            PerlinNoise(seed).also {
                cachedSeed = seed
                cachedPerlin = it
            }
        }
    }
}
