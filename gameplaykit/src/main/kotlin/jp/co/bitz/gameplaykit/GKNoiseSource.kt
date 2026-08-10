package jp.co.bitz.gameplaykit

// Abstract base class for procedural noise generators, mirroring GameplayKit's GKNoiseSource.
// Apple's real GKNoiseSource exposes no public sampling API of its own — only GKNoise samples a
// source, via GKNoise.value(at:) — and subclassing it isn't meaningfully supported by Apple's
// public API either (there's no documented override point), so `sample` here is internal rather
// than a public extension point.
public abstract class GKNoiseSource {
    internal abstract fun sample(position: Vector3): Double
}
