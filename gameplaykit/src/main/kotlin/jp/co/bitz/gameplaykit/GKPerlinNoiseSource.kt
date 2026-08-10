package jp.co.bitz.gameplaykit

/**
 * A [GKCoherentNoiseSource] that generates improved Perlin noise, mirroring GameplayKit's
 * `GKPerlinNoiseSource`. Each octave is weighted by `persistence^octaveIndex` and normalized so
 * the result stays within `[-1, 1]` regardless of `octaveCount`.
 */
public class GKPerlinNoiseSource(
    frequency: Double = 1.0,
    octaveCount: Int = 6,
    /** How much each successive octave's amplitude shrinks by; lower means less high-frequency detail. */
    public var persistence: Double = 0.5,
    lacunarity: Double = 2.0,
    seed: Int = 0,
) : GKCoherentNoiseSource(frequency, octaveCount, lacunarity, seed) {
    override fun sample(position: Vector3): Double {
        val perlin = currentPerlin()
        var total = 0.0
        var amplitude = 1.0
        var freq = frequency
        var maxAmplitude = 0.0

        repeat(octaveCount) {
            total += perlin.noise(position.x * freq, position.y * freq, position.z * freq) * amplitude
            maxAmplitude += amplitude
            amplitude *= persistence
            freq *= lacunarity
        }

        return if (maxAmplitude == 0.0) 0.0 else total / maxAmplitude
    }
}
