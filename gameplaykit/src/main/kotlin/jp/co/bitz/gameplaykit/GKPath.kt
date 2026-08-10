package jp.co.bitz.gameplaykit

/**
 * Holds a 2D polygonal path that a [GKAgent2D] can follow or stay within, mirroring GameplayKit's
 * `GKPath`. 3D paths (Apple also supports `Vector3`-based paths) are out of scope for this port;
 * the steering goals that use a path (`GKGoal.toFollowPath`/`toStayOnPath`) operate in the
 * agent's XY plane.
 */
public class GKPath(
    public val points: List<Vector2>,
    public val radius: Float,
    public val cyclical: Boolean = false,
)
