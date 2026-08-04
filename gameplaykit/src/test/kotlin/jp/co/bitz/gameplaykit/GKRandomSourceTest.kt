package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GKRandomSourceTest {
    @Test
    fun `sharedRandom returns the same instance every time`() {
        assertSame(GKRandomSource.sharedRandom(), GKRandomSource.sharedRandom())
    }

    @Test
    fun `nextInt upperBound stays within bounds`() {
        val source = GKRandomSource()

        repeat(200) {
            val value = source.nextInt(10)
            assertTrue(value in 0 until 10)
        }
    }

    @Test
    fun `nextUniform stays within zero and one`() {
        val source = GKRandomSource()

        repeat(200) {
            val value = source.nextUniform()
            assertTrue(value in 0f..1f)
        }
    }

    @Test
    fun `shuffled preserves the original elements`() {
        val source = GKRandomSource()
        val original = (1..20).toList()

        val shuffled = source.shuffled(original)

        assertTrue(shuffled.toSet() == original.toSet())
        assertTrue(shuffled.size == original.size)
    }
}
