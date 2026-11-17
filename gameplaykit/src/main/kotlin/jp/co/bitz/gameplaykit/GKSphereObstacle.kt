package jp.co.bitz.gameplaykit

/**
 * A spherical obstacle defined by a location and a radius, mirroring GameplayKit's
 * `GKSphereObstacle` — the 3D sibling of [GKCircleObstacle], for [GKGoal.toAvoidObstacles] with a
 * [GKAgent3D]. A [GKAgent2D] avoids it too, as the circle at (`position.x`, `position.z`); see
 * [GKGoal.toAvoidObstacles].
 */
public class GKSphereObstacle(
    public var radius: Float,
    public var position: Vector3 = Vector3(0f, 0f, 0f),
) : GKObstacle()
