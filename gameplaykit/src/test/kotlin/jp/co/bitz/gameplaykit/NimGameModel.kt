package jp.co.bitz.gameplaykit

// Test fixture: the subtraction game "Nim" (two players alternately remove 1-3 stones from a
// pile; whoever takes the last stone wins). Its optimal strategy is well known — from a pile that
// is not a multiple of 4, taking `pile % 4` stones forces a win — which makes it a good exercise
// for GKMinmaxStrategist/GKMonteCarloStrategist: their chosen move can be checked against a
// textbook-known answer instead of just "some legal move".
internal class NimPlayer(override val playerId: Int) : GKGameModelPlayer

internal class NimMove(val amount: Int) : GKGameModelUpdate {
    override var value: Int = 0
}

internal class NimGameModel(
    var pile: Int,
    var activePlayerIndex: Int = 0,
) : GKGameModel {
    private val playerList = listOf(NimPlayer(0), NimPlayer(1))

    override val players: List<GKGameModelPlayer> = playerList
    override val activePlayer: GKGameModelPlayer get() = playerList[activePlayerIndex]

    override fun copy(): GKGameModel = NimGameModel(pile, activePlayerIndex)

    override fun setGameModel(gameModel: GKGameModel) {
        val other = gameModel as NimGameModel
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

    // A real inverse of apply(_:) is required now that both strategists mutate-and-backtrack a
    // shared model instead of branching by copying — see GKGameModel's documentation.
    override fun unapplyGameModelUpdate(gameModelUpdate: GKGameModelUpdate) {
        val move = gameModelUpdate as NimMove
        pile += move.amount
        activePlayerIndex = 1 - activePlayerIndex
    }

    // The pile is empty and it's `player`'s turn: whoever moved last (the other player) took the
    // last stone and won.
    override fun isWin(player: GKGameModelPlayer): Boolean = pile == 0 && player.playerId != activePlayerIndex

    override fun isLoss(player: GKGameModelPlayer): Boolean = pile == 0 && player.playerId == activePlayerIndex

    override fun score(player: GKGameModelPlayer): Int =
        when {
            isWin(player) -> 1
            isLoss(player) -> -1
            else -> 0
        }
}
