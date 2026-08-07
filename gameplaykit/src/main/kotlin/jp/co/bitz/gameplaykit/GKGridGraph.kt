package jp.co.bitz.gameplaykit

// A GKGraph in which movement is constrained to an integer grid, mirroring GameplayKit's GKGridGraph.
// The full width x height grid of GKGridGraphNode is created and connected to its orthogonal (and,
// if `diagonalsAllowed`, diagonal) neighbors up front.
public class GKGridGraph(
    public val gridOrigin: Vector2Int,
    public val gridWidth: Int,
    public val gridHeight: Int,
    public val diagonalsAllowed: Boolean,
) : GKGraph() {
    private val nodesByPosition: MutableMap<Vector2Int, GKGridGraphNode> = mutableMapOf()

    init {
        for (dx in 0 until gridWidth) {
            for (dy in 0 until gridHeight) {
                val position = Vector2Int(gridOrigin.x + dx, gridOrigin.y + dy)
                val node = GKGridGraphNode(position)
                nodesByPosition[position] = node
                add(listOf(node))
            }
        }
        nodesByPosition.values.forEach { connectNodeToAdjacentNodes(it) }
    }

    public fun node(position: Vector2Int): GKGridGraphNode? = nodesByPosition[position]

    // Registers `node` at its gridPosition and links it to whichever of its (up to 8) neighboring
    // grid positions already have a node. Useful both for the initial grid build and for wiring in a
    // node added later at a position outside the original width/height.
    public fun connectNodeToAdjacentNodes(node: GKGridGraphNode) {
        nodesByPosition[node.gridPosition] = node
        neighborOffsets().forEach { (dx, dy) ->
            val neighborPosition = Vector2Int(node.gridPosition.x + dx, node.gridPosition.y + dy)
            nodesByPosition[neighborPosition]?.let { neighbor ->
                if (neighbor !== node) node.addConnections(listOf(neighbor), bidirectional = true)
            }
        }
    }

    private fun neighborOffsets(): List<Pair<Int, Int>> {
        val orthogonal = listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
        val diagonal = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        return if (diagonalsAllowed) orthogonal + diagonal else orthogonal
    }
}
