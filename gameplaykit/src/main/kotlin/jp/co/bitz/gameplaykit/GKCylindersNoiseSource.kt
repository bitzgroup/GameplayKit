package jp.co.bitz.gameplaykit

import kotlin.math.floor
import kotlin.math.sqrt

// A GKNoiseSource whose output consists of concentric cylindrical shells around the Y axis,
// appropriate for wood-grain textures, mirroring GameplayKit's GKCylindersNoiseSource.
public class GKCylindersNoiseSource(
    public var frequency: Double = 1.0,
) : GKNoiseSource() {
    override fun sample(position: Vector3): Double {
        val x = position.x * frequency
        val z = position.z * frequency
        val distanceFromCenter = sqrt(x * x + z * z)
        val distanceFromSmallerRing = distanceFromCenter - floor(distanceFromCenter)
        val distanceFromLargerRing = 1.0 - distanceFromSmallerRing
        val nearest = minOf(distanceFromSmallerRing, distanceFromLargerRing)
        return 1.0 - nearest * RING_WIDTH_SCALE
    }

    private companion object {
        private const val RING_WIDTH_SCALE = 4.0
    }
}
