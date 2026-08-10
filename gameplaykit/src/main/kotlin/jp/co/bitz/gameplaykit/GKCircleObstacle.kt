package jp.co.bitz.gameplaykit

/** A circular obstacle defined by a location and a radius, mirroring GameplayKit's `GKCircleObstacle`. */
public class GKCircleObstacle(
    public var radius: Float,
    public var position: Vector2 = Vector2(0f, 0f),
) : GKObstacle()
