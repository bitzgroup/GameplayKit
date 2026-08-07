package jp.co.bitz.gameplaykit

// An obstacle with an arbitrarily complex polygonal shape, defined by an ordered list of vertices.
// Exposes `vertices` as a Kotlin List rather than mirroring GameplayKit's paired vertexCount/
// vertex(at:) accessors, consistent with this library's idiomatic-Kotlin design principle.
public class GKPolygonObstacle(
    public val vertices: List<Vector2>,
) : GKObstacle()
