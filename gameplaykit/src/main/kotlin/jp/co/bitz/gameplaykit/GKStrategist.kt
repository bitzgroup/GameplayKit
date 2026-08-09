package jp.co.bitz.gameplaykit

// Common contract for a game AI, mirroring GameplayKit's GKStrategist protocol. Implemented by
// GKMinmaxStrategist and GKMonteCarloStrategist.
public interface GKStrategist {
    public var gameModel: GKGameModel?
    public var randomSource: GKRandom?

    // Returns what the strategist indicates is the best move for `gameModel.activePlayer`, or
    // null if there is no game model, no active player, or no legal move.
    public fun bestMoveForActivePlayer(): GKGameModelUpdate?
}
