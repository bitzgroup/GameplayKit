package jp.co.bitz.gameplaykit

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * A strategist that reaches a solution that is probably close to optimal in a deterministic
 * amount of time, mirroring GameplayKit's `GKMonteCarloStrategist`: Monte Carlo tree search with
 * the standard UCT (Upper Confidence bound applied to Trees) selection rule.
 *
 * Matches Apple's own documented implementation strategy — see [GKMinmaxStrategist]'s
 * documentation, which explains this in full: search mutates the one shared [gameModel] in place
 * (`apply` a move while walking down the tree or rolling out a playout, `unapplyGameModelUpdate`
 * it back off again afterward) rather than branching by calling [GKGameModel.copy] at every node.
 * [GKGameModel] implementations *must* provide a correct, real inverse in
 * `unapplyGameModelUpdate` for this search to be sound. By the time [bestMoveForActivePlayer]
 * returns, [gameModel] is back in the exact state it was in when called.
 *
 * Deviation from GameplayKit: Apple doesn't document its own UCT constant, rollout policy, or
 * tie-break rule, so this is contract-conformant (approaches the optimal move as [budget] grows),
 * not bit-identical. Random rollouts are capped at [maxPlayoutDepth] plies as a safety valve for
 * game models with no guaranteed terminal state within that horizon; GameplayKit exposes no such
 * cap because its own implementation detail here isn't documented either. Every reward is tracked
 * from the searching player's fixed perspective (1.0 = that player wins, 0.0 = they lose); nodes
 * reached by an opponent's move are selected to minimize that value rather than maximize it — the
 * same "every other player is a single adversary" generalization [GKMinmaxStrategist] uses —
 * since plain UCB1 would otherwise assume every player is cooperating to win for the searching
 * player.
 */
public class GKMonteCarloStrategist : GKStrategist {
    override var gameModel: GKGameModel? = null
    override var randomSource: GKRandom? = null

    /** Number of playout iterations to run per [bestMoveForActivePlayer] call. */
    public var budget: Int = 100

    /** The UCT exploration constant; higher favors exploring less-visited moves. */
    public var explorationParameter: Double = sqrt(2.0)

    /** Random rollouts stop after this many plies even if the game model has no terminal state within that horizon. */
    public var maxPlayoutDepth: Int = 200

    override fun bestMoveForActivePlayer(): GKGameModelUpdate? {
        val model = gameModel
        val player = model?.activePlayer
        return if (model == null || player == null) {
            null
        } else {
            val source = randomSource ?: GKRandomSource.sharedRandom()
            val root = Node(parent = null, update = null, moverIsForPlayer = true)

            repeat(budget) {
                // Moves applied to `model` this iteration while walking select→expand, in
                // root-to-leaf order — unwound (unapplied, in reverse) once backpropagation is
                // done, so `model` is back at the root position before the next iteration.
                val path = mutableListOf<Node>()
                val leaf = select(root, model, path)
                val expanded = expand(leaf, model, player, source, path)
                val reward = simulate(model, player, source)
                backpropagate(expanded, reward)
                path.asReversed().forEach { model.unapplyGameModelUpdate(checkNotNull(it.update)) }
            }

            root.children.maxByOrNull { it.visits }?.update
        }
    }

    private fun select(
        root: Node,
        model: GKGameModel,
        path: MutableList<Node>,
    ): Node {
        var node = root
        while (node.untriedMoves(model).isEmpty() && node.children.isNotEmpty()) {
            val next = node.children.maxByOrNull { uct(it, node.visits) } ?: break
            model.apply(checkNotNull(next.update))
            path.add(next)
            node = next
        }
        return node
    }

    private fun uct(
        child: Node,
        parentVisits: Int,
    ): Double {
        if (child.visits == 0) return Double.POSITIVE_INFINITY
        val meanReward = child.totalReward / child.visits
        // The move into `child` was chosen by whoever was active at its parent: prefer it when
        // that mover is forPlayer (high reward is good for them), otherwise treat it as an
        // adversary's move and prefer it when reward is low (bad for forPlayer).
        val perspective = if (child.moverIsForPlayer) meanReward else 1.0 - meanReward
        val exploration = explorationParameter * sqrt(ln(parentVisits.toDouble()) / child.visits)
        return perspective + exploration
    }

    private fun expand(
        node: Node,
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
        path: MutableList<Node>,
    ): Node {
        val untried = node.untriedMoves(model)
        if (untried.isEmpty()) return node

        val update = untried.removeAt(source.nextInt(untried.size))
        val mover = model.activePlayer
        model.apply(update)
        val child = Node(node, update, moverIsForPlayer = mover?.playerId == forPlayer.playerId)
        node.children.add(child)
        path.add(child)
        return child
    }

    private fun simulate(
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
    ): Double {
        // Self-contained: every move this random rollout applies to `model` is unapplied again
        // before returning, regardless of the persistent select/expand `path` — a playout is
        // thrown away immediately after scoring it, never added to the tree.
        val playedMoves = mutableListOf<GKGameModelUpdate>()
        var depth = 0
        var reward: Double? = null

        while (reward == null && depth < maxPlayoutDepth) {
            reward = playoutStep(model, forPlayer, source, playedMoves)
            depth++
        }

        playedMoves.asReversed().forEach { model.unapplyGameModelUpdate(it) }
        return reward ?: 0.5
    }

    // Advances one ply of a random playout and returns the terminal reward once the game ends
    // (a win/loss for `forPlayer`, or no legal move left), or null to keep playing. Every applied
    // move is recorded in `playedMoves` so `simulate` can unapply the whole rollout afterward.
    private fun playoutStep(
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
        playedMoves: MutableList<GKGameModelUpdate>,
    ): Double? {
        val player = model.activePlayer
        val updates = player?.let { model.gameModelUpdates(it) }
        return when {
            model.isWin(forPlayer) -> 1.0
            model.isLoss(forPlayer) -> 0.0
            updates.isNullOrEmpty() -> 0.5
            else -> {
                val move = updates[source.nextInt(updates.size)]
                model.apply(move)
                playedMoves.add(move)
                null
            }
        }
    }

    private fun backpropagate(
        start: Node,
        reward: Double,
    ) {
        var node: Node? = start
        while (node != null) {
            node.visits++
            node.totalReward += reward
            node = node.parent
        }
    }

    // `untried` is the set of legal moves not yet expanded into a child. Unlike when each node
    // held its own copied model, there's only ever one shared `model` now, so this can only be
    // computed correctly the moment `model` actually sits at this node's position — which is
    // exactly when `untriedMoves` is first called on it (from `select`/`expand`, always right
    // after navigating here) — so it's filled lazily on that first call and cached from then on,
    // rather than eagerly at construction time.
    private class Node(
        val parent: Node?,
        val update: GKGameModelUpdate?,
        val moverIsForPlayer: Boolean,
    ) {
        var visits: Int = 0
        var totalReward: Double = 0.0
        val children: MutableList<Node> = mutableListOf()
        private var untried: MutableList<GKGameModelUpdate>? = null

        fun untriedMoves(model: GKGameModel): MutableList<GKGameModelUpdate> {
            var moves = untried
            if (moves == null) {
                moves = (model.activePlayer?.let { model.gameModelUpdates(it) } ?: emptyList()).toMutableList()
                untried = moves
            }
            return moves
        }
    }
}
