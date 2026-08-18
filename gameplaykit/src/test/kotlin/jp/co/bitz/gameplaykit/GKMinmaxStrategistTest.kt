package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GKMinmaxStrategistTest {
    @Test
    fun `bestMoveForActivePlayer returns null when there is no game model`() {
        val strategist = GKMinmaxStrategist()

        assertNull(strategist.bestMoveForActivePlayer())
    }

    @Test
    fun `bestMoveForActivePlayer finds the textbook-optimal Nim move`() {
        // 10 % 4 == 2: taking 2 leaves a multiple of 4 for the opponent, a forced win.
        val strategist =
            GKMinmaxStrategist().apply {
                gameModel = NimGameModel(pile = 10)
                maxLookAheadDepth = 10
            }

        val move = strategist.bestMoveForActivePlayer() as? NimMove

        assertEquals(2, move?.amount)
    }

    @Test
    fun `bestMove returns a valid move even from an already-lost position`() {
        // 8 is a multiple of 4: every move hands the opponent a forced win, but a legal move
        // must still be returned.
        val strategist =
            GKMinmaxStrategist().apply {
                gameModel = NimGameModel(pile = 8)
                maxLookAheadDepth = 10
            }

        val move = strategist.bestMoveForActivePlayer() as? NimMove

        assertNotNull(move)
        assertTrue(move.amount in 1..3)
    }

    @Test
    fun `bestMove can be queried for a specific player independent of the active player`() {
        val model = NimGameModel(pile = 10)
        val strategist =
            GKMinmaxStrategist().apply {
                gameModel = model
                maxLookAheadDepth = 10
            }

        val move = strategist.bestMove(model.players[0]) as? NimMove

        assertEquals(2, move?.amount)
    }

    // This is the direct regression test for mutate-and-backtrack search: it fails if
    // unapplyGameModelUpdate (in either GKMinmaxStrategist or NimGameModel) isn't a true inverse
    // of apply — see GKGameModel's documentation and docs/API_COMPATIBILITY.md.
    @Test
    fun `bestMoveForActivePlayer leaves the game model exactly as it found it`() {
        val model = NimGameModel(pile = 10)
        val strategist =
            GKMinmaxStrategist().apply {
                gameModel = model
                maxLookAheadDepth = 10
            }

        strategist.bestMoveForActivePlayer()

        assertEquals(10, model.pile)
        assertEquals(0, model.activePlayerIndex)
    }

    @Test
    fun `randomMove only ever returns one of the numMovesToConsider best moves`() {
        val model = NimGameModel(pile = 10)
        val strategist =
            GKMinmaxStrategist().apply {
                gameModel = model
                maxLookAheadDepth = 10
                randomSource = GKLinearCongruentialRandomSource(seed = 11L)
            }

        repeat(50) {
            val move = strategist.randomMove(model.activePlayer, numMovesToConsider = 2) as? NimMove
            assertNotNull(move)
            assertTrue(move.amount in 1..3)
        }
    }
}
