package jp.co.bitz.gameplaykit

import kotlin.math.abs

/**
 * A [GKCoherentNoiseSource] whose output is similar to Perlin noise but with sharp boundaries,
 * mirroring GameplayKit's `GKRidgedNoiseSource`. Each octave folds its raw Perlin sample through
 * `(1 - abs(n))^2` (the classic "ridged multifractal" transform, producing sharp ridges at zero
 * crossings) before summing with a halving per-octave weight, normalized and rescaled to
 * `[-1, 1]`. GameplayKit doesn't document the exact ridged-multifractal formula it uses (real
 * libnoise-style implementations also apply a spectral weighting/gain step this omits), so this
 * is contract-conformant (sharper-edged than plain Perlin noise), not bit-identical.
 */
public class GKRidgedNoiseSource(
    frequency: Double = 1.0,
    octaveCount: Int = 6,
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
            val ridged = 1.0 - abs(n)
            total += ridged * ridged * amplitude
            maxAmplitude += amplitude
            amplitude *= HALVING_WEIGHT
            freq *= lacunarity
        }

        val normalized = if (maxAmplitude == 0.0) 0.0 else total / maxAmplitude
        return normalized * 2 - 1
    }

    private companion object {
        private const val HALVING_WEIGHT = 0.5
    }
}
