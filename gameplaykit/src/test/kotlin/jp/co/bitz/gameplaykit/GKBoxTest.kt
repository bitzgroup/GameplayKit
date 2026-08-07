package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GKBoxTest {
    @Test
    fun `exposes the min and max it was constructed with`() {
        val box = GKBox(Vector3(0f, 0f, 0f), Vector3(10f, 10f, 10f))

        assertEquals(Vector3(0f, 0f, 0f), box.min)
        assertEquals(Vector3(10f, 10f, 10f), box.max)
    }

    @Test
    fun `equal bounds produce equal boxes`() {
        val a = GKBox(Vector3(0f, 0f, 0f), Vector3(10f, 10f, 10f))
        val b = GKBox(Vector3(0f, 0f, 0f), Vector3(10f, 10f, 10f))
        val c = GKBox(Vector3(0f, 0f, 0f), Vector3(5f, 5f, 5f))

        assertEquals(a, b)
        assertNotEquals(a, c)
    }
}
