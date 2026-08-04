package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GKARC4RandomSourceTest {
    @Test
    fun `same seed produces the same sequence`() {
        val seed = "same-seed".toByteArray()
        val a = GKARC4RandomSource(seed)
        val b = GKARC4RandomSource(seed)

        assertEquals(List(20) { a.nextInt() }, List(20) { b.nextInt() })
    }

    @Test
    fun `different seeds produce different sequences`() {
        val a = GKARC4RandomSource("seed-a".toByteArray())
        val b = GKARC4RandomSource("seed-b".toByteArray())

        assertNotEquals(List(20) { a.nextInt() }, List(20) { b.nextInt() })
    }

    @Test
    fun `reassigning seed reproduces the sequence from that seed`() {
        val seed = "reseed-me".toByteArray()
        val a = GKARC4RandomSource(seed)
        val sequenceFromFresh = List(10) { a.nextInt() }

        a.seed = seed
        val sequenceAfterReseed = List(10) { a.nextInt() }

        assertEquals(sequenceFromFresh, sequenceAfterReseed)
    }

    @Test
    fun `dropValues changes the subsequent sequence`() {
        val seed = "drop-test".toByteArray()
        val withoutDrop = GKARC4RandomSource(seed)
        val withDrop = GKARC4RandomSource(seed)
        withDrop.dropValues(768)

        assertNotEquals(List(10) { withoutDrop.nextInt() }, List(10) { withDrop.nextInt() })
    }

    @Test
    fun `default constructor produces a usable, non-empty seed`() {
        val source = GKARC4RandomSource()

        source.nextInt()
    }
}
