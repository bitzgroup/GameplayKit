package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals

class GKGraphNode3DTest {
    @Test
    fun `cost and estimatedCost equal the Euclidean distance between positions`() {
        val a = GKGraphNode3D(Vector3(0f, 0f, 0f))
        val b = GKGraphNode3D(Vector3(1f, 2f, 2f))

        assertEquals(3f, a.cost(b))
        assertEquals(3f, a.estimatedCost(b))
    }

    @Test
    fun `cost falls back to the base default against a non-3D node`() {
        val a = GKGraphNode3D(Vector3(0f, 0f, 0f))
        val plain = GKGraphNode()

        assertEquals(1f, a.cost(plain))
    }
}
