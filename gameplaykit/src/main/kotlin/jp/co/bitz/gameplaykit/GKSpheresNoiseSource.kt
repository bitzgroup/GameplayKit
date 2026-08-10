package jp.co.bitz.gameplaykit

import kotlin.math.floor
import kotlin.math.sqrt

/**
 * A [GKNoiseSource] whose output consists of concentric shells around the origin, appropriate
 * for wood-grain textures, mirroring GameplayKit's `GKSpheresNoiseSource`.
 */
public class GKSpheresNoiseSource(
    /** How many spherical shells fit per unit of sample-space distance from the origin. */
    public var frequency: Double = 1.0,
) : GKNoiseSource() {
    override fun sample(position: Vector3): Double {
        val x = position.x * frequency
        val y = position.y * frequency
        val z = position.z * frequency
        val distanceFromCenter = sqrt(x * x + y * y + z * z)
        val distanceFromSmallerShell = distanceFromCenter - floor(distanceFromCenter)
        val distanceFromLargerShell = 1.0 - distanceFromSmallerShell
        val nearest = minOf(distanceFromSmallerShell, distanceFromLargerShell)
        return 1.0 - nearest * SHELL_WIDTH_SCALE
    }

    private companion object {
        private const val SHELL_WIDTH_SCALE = 4.0
    }
}
