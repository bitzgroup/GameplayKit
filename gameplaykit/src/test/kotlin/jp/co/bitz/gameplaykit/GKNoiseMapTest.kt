package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GKNoiseMapTest {
    @Test
    fun `value samples a constant noise field uniformly`() {
        val map =
            GKNoiseMap(
                noise = GKNoise(GKConstantNoiseSource(0.75)),
                size = Vector2(4f, 4f),
                origin = Vector2(0f, 0f),
                sampleCount = Vector2Int(5, 5),
            )

        assertEquals(0.75f, map.value(at = Vector2Int(0, 0)))
        assertEquals(0.75f, map.value(at = Vector2Int(4, 4)))
    }

    @Test
    fun `setValue overrides a specific grid point without affecting others`() {
        val map =
            GKNoiseMap(
                noise = GKNoise(GKConstantNoiseSource(0.0)),
                sampleCount = Vector2Int(3, 3),
            )

        map.setValue(9f, at = Vector2Int(1, 1))

        assertEquals(9f, map.value(at = Vector2Int(1, 1)))
        assertEquals(0f, map.value(at = Vector2Int(0, 0)))
    }

    @Test
    fun `interpolatedValue blends between neighboring grid points`() {
        val map =
            GKNoiseMap(
                noise = GKNoise(GKConstantNoiseSource(0.0)),
                sampleCount = Vector2Int(3, 3),
            )
        map.setValue(0f, at = Vector2Int(0, 0))
        map.setValue(10f, at = Vector2Int(1, 0))

        // sampleCount=3 over the default size=1 puts grid points at x = 0, 0.5, 1; x = 0.25 sits
        // halfway between the grid points set above.
        val midpoint = map.interpolatedValue(at = Vector2(0.25f, 0f))

        assertTrue(midpoint in 4f..6f)
    }

    @Test
    fun `seamless wraps the last grid column back to the first`() {
        val map =
            GKNoiseMap(
                noise = GKNoise(GKConstantNoiseSource(0.0)),
                sampleCount = Vector2Int(4, 4),
                seamless = true,
            )
        map.setValue(5f, at = Vector2Int(0, 0))

        assertEquals(5f, map.value(at = Vector2Int(4, 0)))
    }
}
