package jp.co.bitz.gameplaykit

import kotlin.math.abs

// A GKCoherentNoiseSource whose output is similar to Perlin noise but with more rounded features,
// mirroring GameplayKit's GKBillowNoiseSource. Each octave folds its raw Perlin sample through
// `abs(n) * 2 - 1` (the classic "billow" transform) before summing, weighted by
// `persistence^octaveIndex` and normalized to stay within [-1, 1] regardless of octaveCount.
public class GKBillowNoiseSource(
    frequency: Double = 1.0,
    octaveCount: Int = 6,
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
            val n = perlin.noise(position.x * freq, position.y * freq, position.z * freq)
            total += (abs(n) * 2 - 1) * amplitude
            maxAmplitude += amplitude
            amplitude *= persistence
            freq *= lacunarity
        }

        return if (maxAmplitude == 0.0) 0.0 else total / maxAmplitude
    }
}
