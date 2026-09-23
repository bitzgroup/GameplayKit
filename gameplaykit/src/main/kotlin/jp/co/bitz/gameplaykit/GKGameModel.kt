package jp.co.bitz.gameplaykit

/**
 * The current state of a turn-based game, mirroring GameplayKit's `GKGameModel` protocol.
 * Implementations describe their own game's rules; [GKMinmaxStrategist]/[GKMonteCarloStrategist]
 * use only this interface (plus [GKGameModelPlayer]/[GKGameModelUpdate]) to search for a good
 * move, independent of what the game actually is.
 *
 * **[apply]/[unapplyGameModelUpdate] must be true inverses of each other for [GKMinmaxStrategist]**
 * — this is a hard requirement, not just good practice, confirmed by an on-device crash against
 * Apple's real `GKMinmaxStrategist` when it wasn't (`bitzgroup/tic-tac-toe`'s iOS-first
 * implementation order hit this directly): [GKMinmaxStrategist] searches by mutating the one
 * shared model instance handed to it — `apply` a candidate move, recurse, `unapplyGameModelUpdate`
 * it back off before trying the next one — rather than branching by calling [copy] at every search
 * node, matching Apple's own documented `GKMinmaxStrategist` behavior exactly. A
 * [unapplyGameModelUpdate] left as the default no-op below is only safe for a `GKGameModel` that's
 * never handed to [GKMinmaxStrategist]; doing so anyway silently corrupts its search instead of
 * failing loudly. See [GKMinmaxStrategist]'s documentation for the full explanation.
 *
 * **[GKMonteCarloStrategist] has no such requirement.** It searches by branching — calling [copy]
 * at every tree node — so the default no-op [unapplyGameModelUpdate] below is fine for a
 * `GKGameModel` only ever handed to [GKMonteCarloStrategist]. This asymmetry between the two
 * strategists was verified directly, not assumed: `bitzgroup/tic-tac-toe`'s
 * `GKMonteCarloUnapplyCompatibilityTests` ran a `GKGameModel` with a no-op `unapplyGameModelUpdate`
 * against Apple's real `GKMonteCarloStrategist` on-device and confirmed it falls back to
 * `copy(with:)`-based branching and searches correctly — matching developer.apple.com's
 * `unapplyGameModelUpdate(_:)` documentation for strategists in general, which [GKMinmaxStrategist]
 * alone turned out to be a documented exception to. See [GKMonteCarloStrategist]'s documentation.
 *
 * [copy] (this library's stand-in for `NSCopying`, which doesn't exist in Kotlin) must always
 * return an independent deep copy — mutating the copy must never affect the original — since
 * [GKMonteCarloStrategist] calls it internally and any caller may too (along with [setGameModel]),
 * e.g. to snapshot a state before handing it to a strategist.
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
     * a true inverse if this model is ever searched by [GKMinmaxStrategist]; safe to leave as the
     * default no-op for [GKMonteCarloStrategist], which never calls it. See the class-level
     * documentation above.
     */
    public fun unapplyGameModelUpdate(gameModelUpdate: GKGameModelUpdate) {}
}
