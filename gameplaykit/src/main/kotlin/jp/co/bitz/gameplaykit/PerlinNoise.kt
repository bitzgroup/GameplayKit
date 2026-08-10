package jp.co.bitz.gameplaykit

import kotlin.math.floor
import kotlin.random.Random

// Ken Perlin's "improved noise" reference algorithm (permutation table + gradient dot products +
// quintic fade curve), used internally by GKCoherentNoiseSource's subclasses to sample a single
// octave. GameplayKit doesn't document its own noise-generation internals, so this is
// contract-conformant (smooth, seed-reproducible coherent noise in [-1, 1]), not bit-identical.
internal class PerlinNoise(seed: Int) {
    private val permutation: IntArray = buildPermutation(seed)

    fun noise(
        x: Double,
        y: Double,
        z: Double,
    ): Double {
        val xi = floor(x).toInt() and MASK
        val yi = floor(y).toInt() and MASK
        val zi = floor(z).toInt() and MASK
        val xf = x - floor(x)
        val yf = y - floor(y)
        val zf = z - floor(z)
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val a = permutation[xi] + yi
        val aa = permutation[a] + zi
        val ab = permutation[a + 1] + zi
        val b = permutation[xi + 1] + yi
        val ba = permutation[b] + zi
        val bb = permutation[b + 1] + zi

        val x1 = lerp(u, grad(permutation[aa], xf, yf, zf), grad(permutation[ba], xf - 1, yf, zf))
        val x2 = lerp(u, grad(permutation[ab], xf, yf - 1, zf), grad(permutation[bb], xf - 1, yf - 1, zf))
        val y1 = lerp(v, x1, x2)

        val x3 = lerp(u, grad(permutation[aa + 1], xf, yf, zf - 1), grad(permutation[ba + 1], xf - 1, yf, zf - 1))
        val x4 =
            lerp(u, grad(permutation[ab + 1], xf, yf - 1, zf - 1), grad(permutation[bb + 1], xf - 1, yf - 1, zf - 1))
        val y2 = lerp(v, x3, x4)

        return lerp(w, y1, y2)
    }

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(
        t: Double,
        a: Double,
        b: Double,
    ): Double = a + t * (b - a)

    private fun grad(
        hash: Int,
        x: Double,
        y: Double,
        z: Double,
    ): Double {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v =
            if (h < 4) {
                y
            } else if (h == 12 || h == 14) {
                x
            } else {
                z
            }
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }

    private companion object {
        private const val MASK = 255

        private fun buildPermutation(seed: Int): IntArray {
            val base = IntArray(256) { it }
            val random = Random(seed)
            for (i in 255 downTo 1) {
                val j = random.nextInt(i + 1)
                val swap = base[i]
                base[i] = base[j]
                base[j] = swap
            }
            return IntArray(512) { base[it and MASK] }
        }
    }
}
