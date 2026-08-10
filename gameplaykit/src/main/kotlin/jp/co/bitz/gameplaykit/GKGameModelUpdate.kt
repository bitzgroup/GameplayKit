package jp.co.bitz.gameplaykit

/**
 * A valid game move: the minimal data necessary to transition a valid [GKGameModel] into a valid
 * subsequent state, mirroring GameplayKit's `GKGameModelUpdate` protocol. [value] is where a
 * strategist records the move's evaluated score (equivalent to [GKGameModel.score] for the state
 * that results from applying it) as it searches.
 */
public interface GKGameModelUpdate {
    /** The move's evaluated score, as recorded by whichever strategist searched it. */
    public var value: Int
}
