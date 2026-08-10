package jp.co.bitz.gameplaykit

import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A [GKNoiseSource] whose output divides space into cells surrounding seed points, appropriate
 * for crystalline textures, mirroring GameplayKit's `GKVoronoiNoiseSource`. One
 * randomly-displaced feature point is placed per unit cell (scaled by [frequency], jittered
 * within the cell by [displacement]); each sample reports either the distance to the nearest
 * feature point ([distanceEnabled]) or that feature's own pseudo-random value, both
 * deterministic for a given [seed].
 */
public class GKVoronoiNoiseSource(
    /** How many unit cells (each holding one feature point) fit per unit of sample-space distance. */
    public var frequency: Double = 1.0,
    /** How far a feature point may be jittered from its cell's center, as a fraction of the cell size. */
    public var displacement: Double = 1.0,
    /**
     * When true, [sample] reports distance to the nearest feature point; when false, that
     * feature's pseudo-random value.
     */
    public var distanceEnabled: Boolean = false,
    /** Seeds the per-cell feature-point placement, making the field reproducible. */
    public var seed: Int = 0,
) : GKNoiseSource() {
    override fun sample(position: Vector3): Double {
        val point = Point3(position.x * frequency, position.y * frequency, position.z * frequency)
        val nearest = nearestFeature(point)
        return if (distanceEnabled) sqrt(nearest.distanceSq) else nearest.value
    }

    private fun nearestFeature(point: Point3): Feature {
        val cellX = floor(point.x).toInt()
        val cellY = floor(point.y).toInt()
        val cellZ = floor(point.z).toInt()

        val nearest =
            NEIGHBOR_OFFSETS
                .asSequence()
                .map { (dx, dy, dz) -> featureIn(Cell(cellX + dx, cellY + dy, cellZ + dz), point) }
                .minByOrNull { it.distanceSq }
        return checkNotNull(nearest)
    }

    private fun featureIn(
        cell: Cell,
        point: Point3,
    ): Feature {
        val random = cellRandom(cell)
        val featureX = cell.x + 0.5 + (random.nextDouble() - 0.5) * displacement
        val featureY = cell.y + 0.5 + (random.nextDouble() - 0.5) * displacement
        val featureZ = cell.z + 0.5 + (random.nextDouble() - 0.5) * displacement
        val dx = point.x - featureX
        val dy = point.y - featureY
        val dz = point.z - featureZ
        return Feature(dx * dx + dy * dy + dz * dz, random.nextDouble() * 2 - 1)
    }

    private fun cellRandom(cell: Cell): Random {
        val hash = (cell.x * PRIME_X) xor (cell.y * PRIME_Y) xor (cell.z * PRIME_Z) xor seed
        return Random(hash)
    }

    private data class Point3(val x: Double, val y: Double, val z: Double)

    private data class Cell(val x: Int, val y: Int, val z: Int)

    private data class Feature(val distanceSq: Double, val value: Double)

    private companion object {
        private const val PRIME_X = 73856093
        private const val PRIME_Y = 19349663
        private const val PRIME_Z = 83492791

        // The 27 neighboring-cell offsets (including the cell itself) that might contain the
        // nearest feature point to any position within the center cell.
        private val NEIGHBOR_OFFSETS: List<Triple<Int, Int, Int>> =
            (-1..1).flatMap { dx -> (-1..1).flatMap { dy -> (-1..1).map { dz -> Triple(dx, dy, dz) } } }
    }
}
