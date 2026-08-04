package jp.co.bitz.gameplaykit

public interface GKRandom {
    public fun nextInt(): Int

    public fun nextInt(upperBound: Int): Int

    public fun nextUniform(): Float

    public fun nextBool(): Boolean
}
