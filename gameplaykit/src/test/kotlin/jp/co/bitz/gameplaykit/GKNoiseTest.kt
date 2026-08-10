package jp.co.bitz.gameplaykit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKNoiseTest {
    @Test
    fun `a default GKNoise samples zero everywhere`() {
        val noise = GKNoise()

        assertEquals(0f, noise.value(at = Vector2(3f, -7f)))
    }

    @Test
    fun `value samples the source on the X-Z plane`() {
        val noise = GKNoise(GKConstantNoiseSource(2.5))

        assertEquals(2.5f, noise.value(at = Vector2(10f, 10f)))
    }

    @Test
    fun `add sums this noise with another`() {
        val noise = GKNoise(GKConstantNoiseSource(0.3))

        noise.add(GKNoise(GKConstantNoiseSource(0.4)))

        assertTrue(abs(noise.value(at = Vector2(0f, 0f)) - 0.7f) < 0.0001f)
    }

    @Test
    fun `multiply scales this noise by another`() {
        val noise = GKNoise(GKConstantNoiseSource(0.5))

        noise.multiply(GKNoise(GKConstantNoiseSource(4.0)))

        assertEquals(2.0f, noise.value(at = Vector2(0f, 0f)))
    }

    @Test
    fun `getMaximum and getMinimum pick pointwise extremes`() {
        val max = GKNoise(GKConstantNoiseSource(0.2))
        max.getMaximum(GKNoise(GKConstantNoiseSource(0.9)))
        assertEquals(0.9f, max.value(at = Vector2(0f, 0f)))

        val min = GKNoise(GKConstantNoiseSource(0.2))
        min.getMinimum(GKNoise(GKConstantNoiseSource(0.9)))
        assertEquals(0.2f, min.value(at = Vector2(0f, 0f)))
    }

    @Test
    fun `raiseToPower with an exponent`() {
        val noise = GKNoise(GKConstantNoiseSource(2.0))

        noise.raiseToPower(3.0)

        assertEquals(8f, noise.value(at = Vector2(0f, 0f)))
    }

    @Test
    fun `clamp restricts the range`() {
        val noise = GKNoise(GKConstantNoiseSource(5.0))

        noise.clamp(0.0, 1.0)

        assertEquals(1f, noise.value(at = Vector2(0f, 0f)))
    }

    @Test
    fun `applyAbsoluteValue removes the sign`() {
        val noise = GKNoise(GKConstantNoiseSource(-3.0))

        noise.applyAbsoluteValue()

        assertEquals(3f, noise.value(at = Vector2(0f, 0f)))
    }

    @Test
    fun `invert negates the value`() {
        val noise = GKNoise(GKConstantNoiseSource(0.6))

        noise.invert()

        assertTrue(abs(noise.value(at = Vector2(0f, 0f)) - (-0.6f)) < 0.0001f)
    }

    @Test
    fun `move shifts which position of the underlying source is sampled`() {
        val noise = GKNoise(GKCheckerboardNoiseSource(squareSize = 1.0))

        val before = noise.value(at = Vector2(0f, 0f))
        noise.move(by = Vector3(1f, 0f, 0f))
        val after = noise.value(at = Vector2(0f, 0f))

        assertEquals(-before, after)
    }

    @Test
    fun `displace offsets the sample position using other noise fields`() {
        val noise = GKNoise(GKCheckerboardNoiseSource(squareSize = 1.0))

        noise.displace(
            xNoise = GKNoise(GKConstantNoiseSource(1.0)),
            yNoise = GKNoise(GKConstantNoiseSource(0.0)),
            zNoise = GKNoise(GKConstantNoiseSource(0.0)),
        )

        assertEquals(-1f, noise.value(at = Vector2(0f, 0f)))
    }
}
