package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * `GKNoise`'s value clamping and selection constructor, matching what the sample app's
 * `GameplayKitParity` probes (`GKNoiseParityTests`) recorded on iOS/tvOS simulators, plus the
 * libnoise-shaped curve and terrace remapping (GameplayKit's own can't be checked against).
 */
class GKNoiseSelectionRemapTest {
    private fun constant(value: Double) = GKNoise(GKConstantNoiseSource(value))

    private fun GKNoise.sample() = value(at = Vector2(0f, 0f))

    private fun selected(
        components: List<Double>,
        selection: Double,
        boundaries: List<Double>? = null,
        blend: List<Double>? = null,
    ) = GKNoise(components.map(::constant), constant(selection), boundaries, blend).sample()

    private fun assertNear(
        expected: Double,
        actual: Float,
    ) = assertEquals(expected.toFloat(), actual, 1e-5f)

    // Value range

    @Test
    fun `sampled values are clamped to minus one through one`() {
        assertEquals(1f, constant(3.0).sample())
        assertEquals(-1f, constant(-3.0).sample())
        assertNear(-0.3, constant(-0.3).sample())
        assertEquals(-1f, constant(3.0).apply { invert() }.sample())
    }

    @Test
    fun `intermediate results are not clamped`() {
        assertNear(0.5, constant(3.0).apply { add(constant(-2.5)) }.sample())
        assertEquals(1f, constant(3.0).apply { multiply(constant(0.5)) }.sample())
    }

    @Test
    fun `noise maps are clamped too`() {
        val map = { noise: GKNoise ->
            GKNoiseMap(noise, Vector2(1f, 1f), Vector2(0f, 0f), Vector2Int(2, 2), false).value(at = Vector2Int(0, 0))
        }
        assertEquals(1f, map(constant(3.0)))
        assertNear(0.5, map(constant(3.0).apply { add(constant(-2.5)) }))
    }

    // Selection (G6)

    @Test
    fun `a single component is passed through`() {
        assertNear(-0.8, selected(listOf(-0.8), -0.5))
        assertNear(0.8, selected(listOf(0.8), 0.5))
    }

    @Test
    fun `two components are remapped into their half of the range`() {
        assertNear(-0.9, selected(listOf(-0.8, 0.4), -0.5))
        assertNear(-0.5, selected(listOf(0.0, 0.4), -0.5))
        assertNear(-0.1, selected(listOf(0.8, 0.4), -0.5))
        assertNear(0.1, selected(listOf(0.4, -0.8), 0.5))
        assertNear(0.5, selected(listOf(0.4, 0.0), 0.5))
        assertNear(0.9, selected(listOf(0.4, 0.8), 0.5))
    }

    @Test
    fun `a selection value on the boundary picks the upper component`() {
        assertNear(-0.9, selected(listOf(-0.8, 0.8), -0.01))
        assertNear(0.9, selected(listOf(-0.8, 0.8), 0.0))
        assertNear(0.95, selected(listOf(-0.8, 0.8), 0.5, listOf(0.5)))
    }

    @Test
    fun `default boundaries are evenly spaced`() {
        val components = listOf(-0.8, 0.2, 0.8)
        assertNear(-0.933333, selected(components, -0.34))
        assertNear(0.066667, selected(components, -0.33))
        assertNear(0.066667, selected(components, 0.33))
        assertNear(0.933333, selected(components, 0.34))
    }

    @Test
    fun `explicit boundaries set the bands`() {
        assertNear(-0.95, selected(listOf(-0.8, 0.8), -0.9, listOf(-0.5)))
        assertNear(0.25, selected(listOf(0.0, 0.0), 0.9, listOf(-0.5)))
        assertNear(-0.25, selected(listOf(0.0, 0.0), -0.9, listOf(0.5)))
        assertNear(0.55, selected(listOf(0.8, -0.8), 0.9, listOf(0.5)))
        val three = listOf(-0.8, 0.2, 0.8)
        assertNear(-0.95, selected(three, -0.6, listOf(-0.5, 0.5)))
        assertNear(0.1, selected(three, 0.0, listOf(-0.5, 0.5)))
        assertNear(0.95, selected(three, 0.6, listOf(-0.5, 0.5)))
    }

    @Test
    fun `boundary blend distances have no effect`() {
        for (distance in listOf(0.2, 0.5, 1.0, 2.0)) {
            for (s in listOf(
                -0.6,
                -0.1,
                -0.05,
            )) assertNear(-0.5, selected(listOf(0.0, 0.0), s, listOf(0.0), listOf(distance)))
            for (s in listOf(
                0.0,
                0.05,
                0.1,
                0.6,
            )) assertNear(0.5, selected(listOf(0.0, 0.0), s, listOf(0.0), listOf(distance)))
        }
    }

    @Test
    fun `the selected component is unclamped before remapping`() {
        assertEquals(1f, selected(listOf(3.0, 3.0), -0.5))
        assertEquals(1f, selected(listOf(-0.5, 3.0), 0.5))
    }

    @Test
    fun `mismatched boundary counts are rejected`() {
        assertFailsWith<IllegalArgumentException> { selected(listOf(0.0, 0.0), 0.0, emptyList()) }
        assertFailsWith<IllegalArgumentException> { selected(listOf(0.0, 0.0), 0.0, listOf(0.0), emptyList()) }
        assertFailsWith<IllegalArgumentException> { GKNoise(emptyList(), constant(0.0)) }
    }

    // Curve (G7)

    @Test
    fun `a curve passes through its control points and is constant beyond them`() {
        val points = mapOf(-1.0 to -1.0, 0.0 to 0.5, 1.0 to 1.0, -0.5 to 0.0)
        for ((input, output) in points) {
            assertNear(output, constant(input).apply { remapValuesToCurve(points) }.sample())
        }
        assertNear(-1.0, constant(-2.0).apply { remapValuesToCurve(points) }.sample())
        assertNear(1.0, constant(2.0).apply { remapValuesToCurve(points) }.sample())
    }

    @Test
    fun `a two-point curve interpolates smoothly between them`() {
        val points = mapOf(-1.0 to 1.0, 1.0 to -1.0)
        assertNear(0.0, constant(0.0).apply { remapValuesToCurve(points) }.sample())
        val quarter = constant(-0.5).apply { remapValuesToCurve(points) }.sample()
        assertTrue(quarter in 0f..1f)
    }

    // Terraces (G7)

    @Test
    fun `terraces stay near the lower peak and rise steeply into the upper one`() {
        val peaks = listOf(-1.0, 0.0, 1.0)
        val terraced = {
                x: Double,
                inverted: Boolean,
            ->
            constant(x).apply { remapValuesToTerraces(peaks, inverted) }.sample()
        }
        for (peak in peaks) assertNear(peak, terraced(peak, false))
        // alpha 0.5 squared: a quarter of the way up from the lower peak.
        assertNear(-0.75, terraced(-0.5, false))
        assertNear(0.25, terraced(0.5, false))
        // Inverted: a quarter of the way down from the upper peak.
        assertNear(-0.25, terraced(-0.5, true))
        assertNear(0.75, terraced(0.5, true))
    }

    @Test
    fun `terraces are constant beyond the outer peaks and ignore peak order`() {
        val peaks = listOf(0.5, -0.5)
        assertNear(-0.5, constant(-0.9).apply { remapValuesToTerraces(peaks, false) }.sample())
        assertNear(0.5, constant(0.9).apply { remapValuesToTerraces(peaks, false) }.sample())
        assertFailsWith<IllegalArgumentException> { constant(0.0).remapValuesToTerraces(listOf(0.3, 0.3), false) }
    }
}
