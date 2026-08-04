package jp.co.bitz.gameplaykit

import kotlin.random.Random

public open class GKRandomSource internal constructor(
    internal var random: Random,
) : GKRandom {
    public constructor() : this(Random.Default)

    override fun nextInt(): Int = random.nextInt()

    override fun nextInt(upperBound: Int): Int = random.nextInt(upperBound)

    override fun nextUniform(): Float = random.nextFloat()

    override fun nextBool(): Boolean = random.nextBoolean()

    public open fun <T> shuffled(list: List<T>): List<T> = list.shuffled(random)

    public companion object {
        private val shared: GKRandomSource by lazy { GKRandomSource() }

        public fun sharedRandom(): GKRandomSource = shared
    }
}
