package jp.co.bitz.gameplaykit

import kotlin.math.ln
import kotlin.math.sqrt

// A strategist that reaches a solution that is probably close to optimal in a deterministic
// amount of time, mirroring GameplayKit's GKMonteCarloStrategist: Monte Carlo tree search with
// the standard UCT (Upper Confidence bound applied to Trees) selection rule.
//
// Deviation from GameplayKit: Apple doesn't document its own UCT constant, rollout policy, or
// tie-break rule, so this is contract-conformant (approaches the optimal move as `budget` grows),
// not bit-identical. Random rollouts are capped at `maxPlayoutDepth` plies as a safety valve for
// game models with no guaranteed terminal state within that horizon; GameplayKit exposes no such
// cap because its own implementation detail here isn't documented either. Every reward is tracked
// from the searching player's fixed perspective (1.0 = that player wins, 0.0 = they lose); nodes
// reached by an opponent's move are selected to minimize that value rather than maximize it — the
// same "every other player is a single adversary" generalization GKMinmaxStrategist uses — since
// plain UCB1 would otherwise assume every player is cooperating to win for the searching player.
public class GKMonteCarloStrategist : GKStrategist {
    override var gameModel: GKGameModel? = null
    override var randomSource: GKRandom? = null

    // Number of playout iterations to run per bestMoveForActivePlayer() call.
    public var budget: Int = 100

    // The UCT exploration constant; higher favors exploring less-visited moves.
    public var explorationParameter: Double = sqrt(2.0)

    public var maxPlayoutDepth: Int = 200

    override fun bestMoveForActivePlayer(): GKGameModelUpdate? {
        val model = gameModel
        val player = model?.activePlayer
        return if (model == null || player == null) {
            null
        } else {
            val source = randomSource ?: GKRandomSource.sharedRandom()
            val root = Node(model.copy(), parent = null, update = null, moverIsForPlayer = true)

            repeat(budget) {
                val leaf = select(root)
                val expanded = expand(leaf, player, source)
                val reward = simulate(expanded.model, player, source)
                backpropagate(expanded, reward)
            }

            root.children.maxByOrNull { it.visits }?.update
        }
    }

    private fun select(start: Node): Node {
        var node = start
        while (node.untried.isEmpty() && node.children.isNotEmpty()) {
            node = node.children.maxByOrNull { uct(it, node.visits) } ?: break
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
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
    ): Node {
        if (node.untried.isEmpty()) return node

        val update = node.untried.removeAt(source.nextInt(node.untried.size))
        val mover = node.model.activePlayer
        val branch = node.model.copy()
        branch.apply(update)
        val child = Node(branch, node, update, moverIsForPlayer = mover?.playerId == forPlayer.playerId)
        node.children.add(child)
        return child
    }

    private fun simulate(
        start: GKGameModel,
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
    ): Double {
        val model = start.copy()
        var depth = 0
        var reward: Double? = null

        while (reward == null && depth < maxPlayoutDepth) {
            reward = playoutStep(model, forPlayer, source)
            depth++
        }

        return reward ?: 0.5
    }

    // Advances one ply of a random playout and returns the terminal reward once the game ends
    // (a win/loss for `forPlayer`, or no legal move left), or null to keep playing.
    private fun playoutStep(
        model: GKGameModel,
        forPlayer: GKGameModelPlayer,
        source: GKRandom,
    ): Double? {
        val player = model.activePlayer
        val updates = player?.let { model.gameModelUpdates(it) }
        return when {
            model.isWin(forPlayer) -> 1.0
            model.isLoss(forPlayer) -> 0.0
            updates.isNullOrEmpty() -> 0.5
            else -> {
                model.apply(updates[source.nextInt(updates.size)])
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

    // `untried` is the set of legal moves not yet expanded into a child, seeded once (lazily)
    // from the game model's own list so later comparisons never depend on GKGameModelUpdate
    // implementing equals/hashCode (the same objects flow from `untried` into `children`).
    private class Node(
        val model: GKGameModel,
        val parent: Node?,
        val update: GKGameModelUpdate?,
        val moverIsForPlayer: Boolean,
    ) {
        var visits: Int = 0
        var totalReward: Double = 0.0
        val children: MutableList<Node> = mutableListOf()
        val untried: MutableList<GKGameModelUpdate> by lazy {
            (model.activePlayer?.let { model.gameModelUpdates(it) } ?: emptyList()).toMutableList()
        }
    }
}
