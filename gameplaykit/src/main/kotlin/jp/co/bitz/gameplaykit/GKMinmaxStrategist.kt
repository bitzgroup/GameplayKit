package jp.co.bitz.gameplaykit

/**
 * Game AI that evaluates potential game states, scores them, and attempts to maximize its own
 * score while minimizing its opponents', mirroring GameplayKit's `GKMinmaxStrategist`: minimax
 * with alpha-beta pruning, generalized past two players by treating every player other than the
 * one being searched for as a single adversary trying to minimize that player's score.
 *
 * Matches Apple's own documented implementation strategy: this searches by mutating the one
 * shared [gameModel] in place — `apply` a candidate move, recurse, then
 * `unapplyGameModelUpdate` it back off before trying the next one — rather than branching by
 * calling [GKGameModel.copy] at every node. [GKGameModel] implementations *must* provide a
 * correct, real inverse in `unapplyGameModelUpdate`; a no-op there (fine for a `GKGameModel` never
 * used with a strategist) silently corrupts the search here. By the time [bestMoveForActivePlayer]/
 * [bestMove]/[randomMove] returns, [gameModel] is back in the exact state it was in when called —
 * every applied move is unapplied again before returning. See [GKGameModel]'s documentation.
 * Bit-identical move choice isn't guaranteed when multiple moves tie in score (GameplayKit doesn't
 * document its own tie-break rule); this implementation keeps the first-seen move on a tie.
 */
public class GKMinmaxStrategist : GKStrategist {
    override var gameModel: GKGameModel? = null
    override var randomSource: GKRandom? = null

    /** How many plies (half-moves) to search ahead before falling back to [GKGameModel.score]. */
    public var maxLookAheadDepth: Int = 0

    override fun bestMoveForActivePlayer(): GKGameModelUpdate? {
        val player = gameModel?.activePlayer ?: return null
        return bestMove(player)
    }

    /**
     * Returns what the strategist indicates is the best move for [player], regardless of whether
     * [player] is the game model's current active player.
     */
    public fun bestMove(player: GKGameModelPlayer): GKGameModelUpdate? {
        val model = gameModel ?: return null
        return search(model, player, maxLookAheadDepth, Bounds(Int.MIN_VALUE, Int.MAX_VALUE)).update
    }

    /**
     * Returns a random move among the [numMovesToConsider] best moves available to [player],
     * ranked by the same search [bestMove] uses. Adds variety to an otherwise-deterministic AI.
     */
    public fun randomMove(
        player: GKGameModelPlayer,
        numMovesToConsider: Int,
    ): GKGameModelUpdate? {
        val model = gameModel
        val updates = model?.let { it.gameModelUpdates(player) }
        return if (model == null || updates.isNullOrEmpty()) {
            null
        } else {
            val ranked =
                updates
                    .map { update -> update to scoreOfMove(model, player, update) }
                    .sortedByDescending { it.second }
            val pool = ranked.take(numMovesToConsider.coerceAtLeast(1)).map { it.first }
            val source = randomSource ?: GKRandomSource.sharedRandom()
            pool[source.nextInt(pool.size)]
        }
    }

    private fun scoreOfMove(
        model: GKGameModel,
        player: GKGameModelPlayer,
        update: GKGameModelUpdate,
    ): Int {
        model.apply(update)
        val score =
            search(model, player, maxOf(maxLookAheadDepth - 1, 0), Bounds(Int.MIN_VALUE, Int.MAX_VALUE)).score
        model.unapplyGameModelUpdate(update)
        return score
    }

    private data class Result(val update: GKGameModelUpdate?, val score: Int)

    private data class Bounds(val alpha: Int, val beta: Int)

    // Groups a search node's context so the recursive helpers below stay under the parameter-
    // count limit: the state being searched (model/forPlayer/updates/depth) travels together.
    private data class SearchState(
        val model: GKGameModel,
        val forPlayer: GKGameModelPlayer,
        val updates: List<GKGameModelUpdate>,
        val depth: Int,
    )

    private fun search(
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        depth: Int,
        bounds: Bounds,
    ): Result {
        val activePlayer = model.activePlayer
        val updates = activePlayer?.let { model.gameModelUpdates(it) }
        if (isTerminal(model, forPlayer, depth, activePlayer, updates)) {
            return Result(null, model.score(forPlayer))
        }
        checkNotNull(activePlayer)
        checkNotNull(updates)

        val maximizing = activePlayer.playerId == forPlayer.playerId
        return searchBranches(SearchState(model, forPlayer, updates, depth), bounds, maximizing)
    }

    private fun isTerminal(
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        depth: Int,
        activePlayer: GKGameModelPlayer?,
        updates: List<GKGameModelUpdate>?,
    ): Boolean =
        depth == 0 ||
            activePlayer == null ||
            updates.isNullOrEmpty() ||
            model.isWin(forPlayer) ||
            model.isLoss(forPlayer)

    private fun searchBranches(
        state: SearchState,
        bounds: Bounds,
        maximizing: Boolean,
    ): Result {
        var alpha = bounds.alpha
        var beta = bounds.beta
        var best: Result? = null

        for (update in state.updates) {
            state.model.apply(update)
            val score = search(state.model, state.forPlayer, state.depth - 1, Bounds(alpha, beta)).score
            state.model.unapplyGameModelUpdate(update)

            val better = best == null || (if (maximizing) score > best.score else score < best.score)
            if (better) best = Result(update, score)

            if (maximizing) alpha = maxOf(alpha, score) else beta = minOf(beta, score)
            if (beta <= alpha) break
        }

        return checkNotNull(best)
    }
}
