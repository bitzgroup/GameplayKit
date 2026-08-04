package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GKMersenneTwisterRandomSourceTest {
    @Test
    fun `same seed produces the same sequence`() {
        val a = GKMersenneTwisterRandomSource(seed = 42L)
        val b = GKMersenneTwisterRandomSource(seed = 42L)

        assertEquals(List(20) { a.nextInt() }, List(20) { b.nextInt() })
    }

    @Test
    fun `different seeds produce different sequences`() {
        val a = GKMersenneTwisterRandomSource(seed = 1L)
        val b = GKMersenneTwisterRandomSource(seed = 2L)

        assertNotEquals(List(20) { a.nextInt() }, List(20) { b.nextInt() })
    }

    @Test
    fun `reassigning seed reproduces the sequence from that seed`() {
        val a = GKMersenneTwisterRandomSource(seed = 7L)
        val sequenceFromFresh = List(10) { a.nextInt() }

        a.seed = 7L
        val sequenceAfterReseed = List(10) { a.nextInt() }

        assertEquals(sequenceFromFresh, sequenceAfterReseed)
    }

    @Test
    fun `nextInt upperBound stays within bounds across the twist boundary`() {
        val source = GKMersenneTwisterRandomSource(seed = 99L)

        // 312-word state; run past a couple of twists to exercise that boundary too.
        repeat(1000) {
            val value = source.nextInt(50)
            assertTrue(value in 0 until 50)
        }
    }
}
