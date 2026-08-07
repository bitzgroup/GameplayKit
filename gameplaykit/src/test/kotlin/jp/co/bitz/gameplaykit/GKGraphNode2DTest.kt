package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKGraphNode2DTest {
    @Test
    fun `cost and estimatedCost equal the Euclidean distance between positions`() {
        val a = GKGraphNode2D(Vector2(0f, 0f))
        val b = GKGraphNode2D(Vector2(3f, 4f))

        assertEquals(5f, a.cost(b))
        assertEquals(5f, a.estimatedCost(b))
    }

    @Test
    fun `cost falls back to the base default against a non-2D node`() {
        val a = GKGraphNode2D(Vector2(0f, 0f))
        val plain = GKGraphNode()

        assertEquals(1f, a.cost(plain))
    }
}
