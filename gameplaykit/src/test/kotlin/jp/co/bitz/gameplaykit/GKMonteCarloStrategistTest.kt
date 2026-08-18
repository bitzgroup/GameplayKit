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

    // This is the direct regression test for mutate-and-backtrack search: it fails if
    // unapplyGameModelUpdate (in either GKMonteCarloStrategist or NimGameModel) isn't a true
    // inverse of apply — see GKGameModel's documentation and docs/API_COMPATIBILITY.md.
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
}
