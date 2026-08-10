package jp.co.bitz.gameplaykit

// The current state of a turn-based game, mirroring GameplayKit's GKGameModel protocol.
// Implementations describe their own game's rules; GKMinmaxStrategist/GKMonteCarloStrategist use
// only this interface (plus GKGameModelPlayer/GKGameModelUpdate) to search for a good move,
// independent of what the game actually is.
//
// Deviation from GameplayKit: `copy()` here (this library's stand-in for NSCopying, which doesn't
// exist in Kotlin) must return an independent deep copy — mutating the copy must never affect the
// original. Both strategists in this library always branch the search tree by copying rather than
// by calling `apply` then `unapplyGameModelUpdate` to backtrack, so a correct `copy()` is what
// keeps the search sound; `unapplyGameModelUpdate` is kept only for API parity and is never called
// internally (Apple documents GKMinMaxStrategist's own implementation as backtracking via
// unapply for space efficiency, which this port does not attempt to replicate).
public interface GKGameModel {
    public val players: List<GKGameModelPlayer>?
    public val activePlayer: GKGameModelPlayer?

    public fun copy(): GKGameModel

    public fun setGameModel(gameModel: GKGameModel)

    public fun gameModelUpdates(player: GKGameModelPlayer): List<GKGameModelUpdate>?

    public fun apply(gameModelUpdate: GKGameModelUpdate)

    public fun score(player: GKGameModelPlayer): Int = 0

    public fun isWin(player: GKGameModelPlayer): Boolean = false

    public fun isLoss(player: GKGameModelPlayer): Boolean = false

    public fun unapplyGameModelUpdate(gameModelUpdate: GKGameModelUpdate) {}
}
