package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GKLinearCongruentialRandomSourceTest {
    @Test
    fun `same seed produces the same sequence`() {
        val a = GKLinearCongruentialRandomSource(seed = 42L)
        val b = GKLinearCongruentialRandomSource(seed = 42L)

        val sequenceA = List(20) { a.nextInt() }
        val sequenceB = List(20) { b.nextInt() }

        assertEquals(sequenceA, sequenceB)
    }

    @Test
    fun `different seeds produce different sequences`() {
        val a = GKLinearCongruentialRandomSource(seed = 1L)
        val b = GKLinearCongruentialRandomSource(seed = 2L)

        assertNotEquals(List(20) { a.nextInt() }, List(20) { b.nextInt() })
    }

    @Test
    fun `reassigning seed reproduces the sequence from that seed`() {
        val a = GKLinearCongruentialRandomSource(seed = 7L)
        val sequenceFromFresh = List(10) { a.nextInt() }

        a.seed = 7L
        val sequenceAfterReseed = List(10) { a.nextInt() }

        assertEquals(sequenceFromFresh, sequenceAfterReseed)
    }

    @Test
    fun `matches java-util-Random bit for bit, per GameplayKit's documented algorithm`() {
        val source = GKLinearCongruentialRandomSource(seed = 12345L)
        val reference = java.util.Random(12345L)

        repeat(20) {
            assertEquals(reference.nextInt(), source.nextInt())
        }
    }
}
