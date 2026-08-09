package jp.co.bitz.gameplaykit

// A valid game move: the minimal data necessary to transition a valid GKGameModel into a valid
// subsequent state, mirroring GameplayKit's GKGameModelUpdate protocol. `value` is where a
// strategist records the move's evaluated score (equivalent to GKGameModel.score(forPlayer:) for
// the state that results from applying it) as it searches.
public interface GKGameModelUpdate {
    public var value: Int
}
