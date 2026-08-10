package jp.co.bitz.gameplaykit

/**
 * The current state of a turn-based game, mirroring GameplayKit's `GKGameModel` protocol.
 * Implementations describe their own game's rules; [GKMinmaxStrategist]/[GKMonteCarloStrategist]
 * use only this interface (plus [GKGameModelPlayer]/[GKGameModelUpdate]) to search for a good
 * move, independent of what the game actually is.
 *
 * Deviation from GameplayKit: [copy] here (this library's stand-in for `NSCopying`, which doesn't
 * exist in Kotlin) must return an independent deep copy — mutating the copy must never affect the
 * original. Both strategists in this library always branch the search tree by copying rather than
 * by calling [apply] then [unapplyGameModelUpdate] to backtrack, so a correct [copy] is what
 * keeps the search sound; [unapplyGameModelUpdate] is kept only for API parity and is never called
 * internally (Apple documents `GKMinMaxStrategist`'s own implementation as backtracking via
 * unapply for space efficiency, which this port does not attempt to replicate).
 */
public interface GKGameModel {
    /** Every player in the game, or `null` if the game has no player concept. */
    public val players: List<GKGameModelPlayer>?

    /** The player whose turn it currently is, or `null` if the game is over or has no player concept. */
    public val activePlayer: GKGameModelPlayer?

    /** Returns an independent deep copy of this game state; mutating the copy must never affect the original. */
    public fun copy(): GKGameModel

    /** Copies [gameModel]'s state into this instance, so this instance mirrors it exactly. */
    public fun setGameModel(gameModel: GKGameModel)

    /** Every legal move [player] can make from this state, or `null` if there are none. */
    public fun gameModelUpdates(player: GKGameModelPlayer): List<GKGameModelUpdate>?

    /** Mutates this state as if [gameModelUpdate] had just been played. */
    public fun apply(gameModelUpdate: GKGameModelUpdate)

    /** How favorable this state is for [player]; higher is better. `0` by default. */
    public fun score(player: GKGameModelPlayer): Int = 0

    /** Whether this state is a win for [player]. `false` by default. */
    public fun isWin(player: GKGameModelPlayer): Boolean = false

    /** Whether this state is a loss for [player]. `false` by default. */
    public fun isLoss(player: GKGameModelPlayer): Boolean = false

    /** Reverses the effect of [apply]\([gameModelUpdate]\). No-op by default; see the class-level deviation note. */
    public fun unapplyGameModelUpdate(gameModelUpdate: GKGameModelUpdate) {}
}
