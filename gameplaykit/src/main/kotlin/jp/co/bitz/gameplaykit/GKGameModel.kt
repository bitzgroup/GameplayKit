package jp.co.bitz.gameplaykit

/**
 * The current state of a turn-based game, mirroring GameplayKit's `GKGameModel` protocol.
 * Implementations describe their own game's rules; [GKMinmaxStrategist]/[GKMonteCarloStrategist]
 * use only this interface (plus [GKGameModelPlayer]/[GKGameModelUpdate]) to search for a good
 * move, independent of what the game actually is.
 *
 * **[apply]/[unapplyGameModelUpdate] must be true inverses of each other** — this is a hard
 * requirement, not just good practice, matching Apple's own documented `GKMinmaxStrategist`
 * behavior: both strategists in this library search by mutating the one shared model instance
 * handed to them — `apply` a candidate move, recurse/roll out, `unapplyGameModelUpdate` it back
 * off before trying the next one — rather than branching by calling [copy] at every search node.
 * A [unapplyGameModelUpdate] left as the default no-op below is only safe for a `GKGameModel`
 * that's never handed to [GKMinmaxStrategist]/[GKMonteCarloStrategist]; doing so anyway silently
 * corrupts their search instead of failing loudly. See [GKMinmaxStrategist]'s documentation for
 * the full explanation, including why this library settled on mutate-and-backtrack over
 * copy-branching despite the extra implementation burden it places on [GKGameModel] authors.
 *
 * [copy] (this library's stand-in for `NSCopying`, which doesn't exist in Kotlin) must still
 * return an independent deep copy — mutating the copy must never affect the original — even
 * though neither strategist calls it internally anymore: it (along with [setGameModel]) remains
 * part of this protocol for API parity with Apple's own `GKGameModel`/`NSCopying`, and for
 * general-purpose use by callers (e.g. snapshotting a state before handing it to a strategist).
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

    /**
     * Reverses the effect of [apply]\([gameModelUpdate]\). No-op by default — override this with
     * a true inverse if this model is ever searched by [GKMinmaxStrategist]/
     * [GKMonteCarloStrategist]; see the class-level documentation above.
     */
    public fun unapplyGameModelUpdate(gameModelUpdate: GKGameModelUpdate) {}
}
