package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GKMonteCarloStrategistTest {
    @Test
    fun `bestMoveForActivePlayer returns null when there is no game model`() {
        val strategist = GKMonteCarloStrategist()

        assertNull(strategist.bestMoveForActivePlayer())
    }

    @Test
    fun `bestMoveForActivePlayer returns a legal move`() {
        val strategist =
            GKMonteCarloStrategist().apply {
                gameModel = NimGameModel(pile = 10)
                budget = 200
                randomSource = GKLinearCongruentialRandomSource(seed = 3L)
            }

        val move = strategist.bestMoveForActivePlayer() as? NimMove

        assertNotNull(move)
        assertTrue(move.amount in 1..3)
    }

    // GKMonteCarloStrategist branches by copying rather than mutating gameModel in place, so this
    // should hold trivially — this pins that behavior as a regression test.
    @Test
    fun `bestMoveForActivePlayer leaves the game model exactly as it found it`() {
        val model = NimGameModel(pile = 10)
        val strategist =
            GKMonteCarloStrategist().apply {
                gameModel = model
                budget = 200
                randomSource = GKLinearCongruentialRandomSource(seed = 3L)
            }

        strategist.bestMoveForActivePlayer()

        assertEquals(10, model.pile)
        assertEquals(0, model.activePlayerIndex)
    }

    // MCTS is probabilistic, so a single run isn't asserted against the textbook-optimal move
    // (see GKMinmaxStrategistTest) — instead, across many independently-seeded searches with a
    // reasonable budget, the optimal move (take 2, from a pile of 10) should come out on top more
    // often than not.
    @Test
    fun `bestMoveForActivePlayer converges on the optimal Nim move most of the time`() {
        val trials = 20
        val optimalCount =
            (0 until trials).count { seed ->
                val strategist =
                    GKMonteCarloStrategist().apply {
                        gameModel = NimGameModel(pile = 10)
                        budget = 300
                        randomSource = GKLinearCongruentialRandomSource(seed = seed.toLong())
                    }
                (strategist.bestMoveForActivePlayer() as? NimMove)?.amount == 2
            }

        assertTrue(optimalCount > trials / 2)
    }

    // Direct regression test for the fix this covers: GKMonteCarloStrategist must search
    // correctly even when unapplyGameModelUpdate is left as GKGameModel's default no-op, matching
    // Apple's real GKMonteCarloStrategist (verified on-device against Apple's actual
    // GameplayKit.framework by bitzgroup/tic-tac-toe's GKMonteCarloUnapplyCompatibilityTests —
    // see GKGameModel's and this class's KDoc). Before this fix, the strategist mutated the one
    // shared gameModel and relied on unapplyGameModelUpdate to back it out again, so a model like
    // this one would have left the pile corrupted (or gone negative) by the time search returned.
    @Test
    fun `bestMoveForActivePlayer works correctly without a real unapplyGameModelUpdate`() {
        val model = NimGameModelWithoutUnapply(pile = 10)
        val strategist =
            GKMonteCarloStrategist().apply {
                gameModel = model
                budget = 300
                randomSource = GKLinearCongruentialRandomSource(seed = 3L)
            }

        val move = strategist.bestMoveForActivePlayer() as? NimMove

        assertNotNull(move)
        assertTrue(move.amount in 1..3)
        assertEquals(10, model.pile)
        assertEquals(0, model.activePlayerIndex)
    }
}

// NimGameModel with unapplyGameModelUpdate deliberately left as GKGameModel's default no-op —
// the fixture for the regression test above.
private class NimGameModelWithoutUnapply(
    var pile: Int,
    var activePlayerIndex: Int = 0,
) : GKGameModel {
    private val playerList = listOf(NimPlayer(0), NimPlayer(1))

    override val players: List<GKGameModelPlayer> = playerList
    override val activePlayer: GKGameModelPlayer get() = playerList[activePlayerIndex]

    override fun copy(): GKGameModel = NimGameModelWithoutUnapply(pile, activePlayerIndex)

    override fun setGameModel(gameModel: GKGameModel) {
        val other = gameModel as NimGameModelWithoutUnapply
        pile = other.pile
        activePlayerIndex = other.activePlayerIndex
    }

    override fun gameModelUpdates(player: GKGameModelPlayer): List<GKGameModelUpdate>? {
        if (player.playerId != activePlayerIndex || pile == 0) return null
        return (1..minOf(3, pile)).map { NimMove(it) }
    }

    override fun apply(gameModelUpdate: GKGameModelUpdate) {
        val move = gameModelUpdate as NimMove
        pile -= move.amount
        activePlayerIndex = 1 - activePlayerIndex
    }

    // Deliberately NOT overridden — this is the whole point of this fixture.

    override fun isWin(player: GKGameModelPlayer): Boolean = pile == 0 && player.playerId != activePlayerIndex

    override fun isLoss(player: GKGameModelPlayer): Boolean = pile == 0 && player.playerId == activePlayerIndex

    override fun score(player: GKGameModelPlayer): Int =
        when {
            isWin(player) -> 1
            isLoss(player) -> -1
            else -> 0
        }
}
