package jp.co.bitz.gameplaykit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Mirrors the tic-tac-toe sample's `GKRTreeParityTests` probe of Apple's `GKRTree`. */
class GKRTreeTest {
    private fun v(
        x: Float,
        y: Float,
    ) = Vector2(x, y)

    private fun GKRTree<Int>.query(
        min: Vector2,
        max: Vector2,
    ): List<Int> = elements(min, max)

    /** Elements 0 until [count] as 1x1 rects in a row along x, 10 apart. */
    private fun row(
        count: Int = 12,
        strategy: GKRTreeSplitStrategy = GKRTreeSplitStrategy.LINEAR,
    ): GKRTree<Int> =
        GKRTree<Int>(maxNumberOfChildren = 3).apply {
            for (n in 0 until count) add(n, v(n * 10f, 0f), v(n * 10f + 1f, 1f), strategy)
        }

    @Test
    fun `a query returns only elements whose rects it fully contains`() {
        val tree = GKRTree<Int>(maxNumberOfChildren = 3)
        tree.add(1, v(0f, 0f), v(2f, 2f), GKRTreeSplitStrategy.HALVE)
        assertEquals(listOf(1), tree.query(v(-1f, -1f), v(3f, 3f)))
        assertEquals(listOf(1), tree.query(v(0f, 0f), v(2f, 2f)))
        assertEquals(emptyList(), tree.query(v(1f, 1f), v(3f, 3f)))
        assertEquals(emptyList(), tree.query(v(0.5f, 0.5f), v(1f, 1f)))
        assertEquals(emptyList(), tree.query(v(1f, 1f), v(1f, 1f)))
        assertEquals(emptyList(), tree.query(v(2f, 0f), v(3f, 2f)))
        assertEquals(emptyList(), tree.query(v(-1f, -1f), v(3f, 1f)))
    }

    @Test
    fun `a query deep in a larger tree also returns only fully contained elements`() {
        for (strategy in GKRTreeSplitStrategy.entries) {
            val tree = GKRTree<Int>(maxNumberOfChildren = 3)
            for (i in 0 until 30) {
                val x = (i % 6) * 10f
                val y = (i / 6) * 10f
                tree.add(i, v(x, y), v(x + 2f, y + 2f), strategy)
            }
            assertEquals((0 until 30).toSet(), tree.query(v(-1f, -1f), v(100f, 100f)).toSet(), "$strategy")
            assertEquals(30, tree.query(v(-1f, -1f), v(100f, 100f)).size, "$strategy")
            assertEquals(listOf(7), tree.query(v(9f, 9f), v(21f, 21f)), "$strategy")
        }
    }

    @Test
    fun `an inverted query rect returns nothing`() {
        assertEquals(emptyList(), row().query(v(200f, 9f), v(-9f, -9f)))
    }

    @Test
    fun `elements that fit in one node come back in insertion order`() {
        val tree = GKRTree<Int>(maxNumberOfChildren = 3)
        for (n in listOf(2, 0, 1)) tree.add(n, v(n.toFloat(), 0f), v(n + 1f, 1f), GKRTreeSplitStrategy.HALVE)
        assertEquals(listOf(2, 0, 1), tree.query(v(-9f, -9f), v(9f, 9f)))
    }

    @Test
    fun `every split strategy keeps every element findable`() {
        for (strategy in GKRTreeSplitStrategy.entries) {
            val tree = row(8, strategy)
            assertEquals((0 until 8).toSet(), tree.query(v(-9f, -9f), v(200f, 9f)).toSet(), "$strategy")
            assertEquals(setOf(2, 3, 4), tree.query(v(15f, -1f), v(45f, 2f)).toSet(), "$strategy")
        }
    }

    @Test
    fun `two children per node is enough and one is rejected`() {
        val tree = GKRTree<Int>(maxNumberOfChildren = 2)
        for (n in 0 until 4) tree.add(n, v(n.toFloat(), 0f), v(n + 1f, 1f), GKRTreeSplitStrategy.HALVE)
        assertEquals((0 until 4).toSet(), tree.query(v(-9f, -9f), v(99f, 9f)).toSet())
        // Apple's crashes on the second insert; this one refuses up front.
        assertFailsWith<IllegalArgumentException> { GKRTree<Int>(maxNumberOfChildren = 1) }
    }

    @Test
    fun `queryReserve defaults to one and does not change results`() {
        val tree = row(5)
        assertEquals(1, tree.queryReserve)
        val before = tree.query(v(-9f, -9f), v(200f, 9f))
        tree.queryReserve = 0
        assertEquals(before, tree.query(v(-9f, -9f), v(200f, 9f)))
        tree.queryReserve = 100
        assertEquals(before, tree.query(v(-9f, -9f), v(200f, 9f)))
    }

    @Test
    fun `the same element can be added more than once`() {
        val tree = GKRTree<Int>(maxNumberOfChildren = 3)
        tree.add(3, v(0f, 0f), v(1f, 1f), GKRTreeSplitStrategy.HALVE)
        tree.add(3, v(0f, 0f), v(1f, 1f), GKRTreeSplitStrategy.HALVE)
        tree.add(3, v(5f, 5f), v(6f, 6f), GKRTreeSplitStrategy.HALVE)
        assertEquals(listOf(3, 3, 3), tree.query(v(-9f, -9f), v(9f, 9f)))
        assertEquals(listOf(3, 3), tree.query(v(-1f, -1f), v(2f, 2f)))
    }

    @Test
    fun `removing takes the element's first record in a node whatever its rect`() {
        val tree = GKRTree<Int>(maxNumberOfChildren = 3)
        tree.add(3, v(0f, 0f), v(1f, 1f), GKRTreeSplitStrategy.HALVE)
        tree.add(3, v(5f, 5f), v(6f, 6f), GKRTreeSplitStrategy.HALVE)
        tree.remove(3, v(5f, 5f), v(6f, 6f))
        assertEquals(emptyList(), tree.query(v(-1f, -1f), v(2f, 2f)))
        assertEquals(listOf(3), tree.query(v(4f, 4f), v(7f, 7f)))

        val single = GKRTree<Int>(maxNumberOfChildren = 3)
        single.add(5, v(0f, 0f), v(1f, 1f), GKRTreeSplitStrategy.HALVE)
        single.remove(5, v(50f, 50f), v(60f, 60f))
        assertEquals(emptyList(), single.query(v(-9f, -9f), v(9f, 9f)))
    }

    @Test
    fun `removal only descends into nodes whose bounds contain the rect`() {
        val cases =
            listOf(
                Triple("exact", v(0f, 0f) to v(1f, 1f), true),
                Triple("inside the element", v(0.2f, 0.2f) to v(0.8f, 0.8f), true),
                Triple("beside the element, inside its node", v(2f, 0f) to v(3f, 1f), true),
                Triple("sticking out of the node in y", v(0.5f, 0.5f) to v(5f, 5f), false),
                Triple("covering the whole tree", v(-1f, -1f) to v(200f, 2f), false),
                Triple("in another node", v(110f, 0f) to v(111f, 1f), false),
            )
        for ((label, rect, removed) in cases) {
            val tree = row()
            tree.remove(0, rect.first, rect.second)
            val left = tree.query(v(-9f, -9f), v(200f, 9f))
            assertEquals(!removed, 0 in left, label)
            assertEquals(if (removed) 11 else 12, left.size, label)
        }
    }

    @Test
    fun `removing an element that is not there changes nothing`() {
        val tree = row()
        tree.remove(99, v(0f, 0f), v(200f, 1f))
        assertEquals((0 until 12).toSet(), tree.query(v(-9f, -9f), v(200f, 9f)).toSet())
        assertEquals(12, tree.query(v(-9f, -9f), v(200f, 9f)).size)
    }

    @Test
    fun `removing every element leaves a usable empty tree`() {
        for (strategy in GKRTreeSplitStrategy.entries) {
            val tree = row(12, strategy)
            for (n in 0 until 12) tree.remove(n, v(n * 10f, 0f), v(n * 10f + 1f, 1f))
            assertEquals(emptyList(), tree.query(v(-9f, -9f), v(200f, 9f)), "$strategy")
            tree.add(42, v(0f, 0f), v(1f, 1f), strategy)
            assertEquals(listOf(42), tree.query(v(-9f, -9f), v(200f, 9f)), "$strategy")
        }
    }
}
