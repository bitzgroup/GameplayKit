package jp.co.bitz.gameplaykit

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A Gaussian (normal) distribution of integers, mirroring GameplayKit's `GKGaussianDistribution`.
 * Values cluster around [mean] with the given [deviation], clamped to `[lowestValue,
 * highestValue]`. Sampled with the Marsaglia polar method, a standard, textbook-correct Gaussian
 * sampler; GameplayKit's exact internal algorithm is undocumented, so this matches the documented
 * mean/deviation contract rather than being bit-for-bit identical to Apple's implementation.
 */
public class GKGaussianDistribution private constructor(
    randomSource: GKRandom,
    lowestValue: Int,
    highestValue: Int,
    mean: Float,
    deviation: Float,
) : GKRandomDistribution(randomSource, lowestValue, highestValue) {
    /**
     * Creates a distribution over `[lowestValue, highestValue]`, centered with
     * `mean = (lowestValue + highestValue) / 2`.
     */
    public constructor(randomSource: GKRandom, lowestValue: Int, highestValue: Int) : this(
        randomSource,
        lowestValue,
        highestValue,
        mean = (lowestValue + highestValue) / 2f,
        deviation = (highestValue - lowestValue) / 6f,
    )

    /**
     * Creates a distribution centered on [mean] with the given [deviation], with `lowestValue`/
     * `highestValue` derived as three deviations either side of the mean (100% of values fall
     * within three deviations of the mean, so the range spans six deviations).
     */
    public constructor(randomSource: GKRandom, mean: Float, deviation: Float) : this(
        randomSource,
        lowestValue = floor(mean - 3f * deviation).toInt(),
        highestValue = ceil(mean + 3f * deviation).toInt(),
        mean = mean,
        deviation = deviation,
    )

    /** The center of the distribution. */
    public var mean: Float = mean

    /** The standard deviation of the distribution. */
    public var deviation: Float = deviation

    private var spareSample: Float? = null

    override fun nextInt(): Int = nextGaussianSample().roundToInt().coerceIn(lowestValue, highestValue)

    private fun nextGaussianSample(): Float {
        val spare = spareSample
        if (spare != null) {
            spareSample = null
            return mean + deviation * spare
        }

        var u: Float
        var v: Float
        var s: Float
        do {
            u = randomSource.nextUniform() * 2f - 1f
            v = randomSource.nextUniform() * 2f - 1f
            s = u * u + v * v
        } while (s >= 1f || s == 0f)

        val multiplier = sqrt(-2f * ln(s) / s)
        spareSample = v * multiplier
        return mean + deviation * (u * multiplier)
    }
}
