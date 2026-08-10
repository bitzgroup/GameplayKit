package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// GKGameModel's score/isWin/isLoss/unapplyGameModelUpdate members are optional in GameplayKit's
// own protocol; this checks the default implementations a minimal conformer gets for free.
class GKGameModelTest {
    private class MinimalGameModel : GKGameModel {
        override val players: List<GKGameModelPlayer>? = null
        override val activePlayer: GKGameModelPlayer? = null

        override fun copy(): GKGameModel = MinimalGameModel()

        override fun setGameModel(gameModel: GKGameModel) = Unit

        override fun gameModelUpdates(player: GKGameModelPlayer): List<GKGameModelUpdate>? = null

        override fun apply(gameModelUpdate: GKGameModelUpdate) = Unit
    }

    private val player = NimPlayer(0)

    @Test
    fun `score defaults to zero`() {
        assertEquals(0, MinimalGameModel().score(player))
    }

    @Test
    fun `isWin defaults to false`() {
        assertFalse(MinimalGameModel().isWin(player))
    }

    @Test
    fun `isLoss defaults to false`() {
        assertFalse(MinimalGameModel().isLoss(player))
    }

    @Test
    fun `unapplyGameModelUpdate defaults to a no-op`() {
        val move = NimMove(1)

        // Should not throw.
        MinimalGameModel().unapplyGameModelUpdate(move)
    }
}
