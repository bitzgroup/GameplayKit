package jp.co.bitz.gameplaykit

// A GKNoiseSource whose output is a single, unvarying value, mirroring GameplayKit's
// GKConstantNoiseSource.
public class GKConstantNoiseSource(
    public var value: Double = 0.0,
) : GKNoiseSource() {
    override fun sample(position: Vector3): Double = value
}
