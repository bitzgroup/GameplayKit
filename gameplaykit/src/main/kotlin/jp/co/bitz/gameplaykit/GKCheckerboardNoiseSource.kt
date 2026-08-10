package jp.co.bitz.gameplaykit

import kotlin.math.floor

/**
 * A [GKNoiseSource] whose output consists of alternating black and white squares, mirroring
 * GameplayKit's `GKCheckerboardNoiseSource`. [squareSize] is the edge length of one square; the
 * result is -1 or 1 depending on the parity of the cell `(x + y + z)` falls in.
 */
public class GKCheckerboardNoiseSource(
    /** The edge length of one square in the checkerboard pattern. */
    public var squareSize: Double = 1.0,
) : GKNoiseSource() {
    override fun sample(position: Vector3): Double {
        val size = if (squareSize == 0.0) 1.0 else squareSize
        val cellX = floor(position.x / size).toInt()
        val cellY = floor(position.y / size).toInt()
        val cellZ = floor(position.z / size).toInt()
        return if ((cellX + cellY + cellZ) % 2 == 0) 1.0 else -1.0
    }
}
