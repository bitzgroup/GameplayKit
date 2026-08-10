package jp.co.bitz.gameplaykit

/** A uniquely-identified player of a game, mirroring GameplayKit's `GKGameModelPlayer` protocol. */
public interface GKGameModelPlayer {
    /** This player's unique identifier within the game. */
    public val playerId: Int
}
