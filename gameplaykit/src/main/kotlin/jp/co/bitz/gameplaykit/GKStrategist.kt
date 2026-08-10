package jp.co.bitz.gameplaykit

/**
 * Common contract for a game AI, mirroring GameplayKit's `GKStrategist` protocol. Implemented by
 * [GKMinmaxStrategist] and [GKMonteCarloStrategist].
 */
public interface GKStrategist {
    /** The game state this strategist searches. `null` until assigned. */
    public var gameModel: GKGameModel?

    /** Used to break ties/make randomized choices; `null` means "no randomization" for strategists that support it. */
    public var randomSource: GKRandom?

    /**
     * Returns what the strategist indicates is the best move for `gameModel.activePlayer`, or
     * `null` if there is no game model, no active player, or no legal move.
     */
    public fun bestMoveForActivePlayer(): GKGameModelUpdate?
}
