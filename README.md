# GameplayKit

A Kotlin library for Android that implements the same API as Apple's
[GameplayKit](https://developer.apple.com/documentation/gameplaykit) framework — entity-component
architecture, state machines, pathfinding, agent steering behaviors, rule systems, randomization,
decision trees, minmax/Monte Carlo game AI, spatial partitioning, and noise generation.

The goal is API and behavioral parity with GameplayKit's types (`GKEntity`, `GKStateMachine`,
`GKAgent`, `GKGraph`, and so on), expressed in idiomatic Kotlin rather than a literal port. See
[`docs/ROADMAP.md`](docs/ROADMAP.md) for the design principles behind that adaptation and the full
implementation progress checklist, and [`docs/API_COMPATIBILITY.md`](docs/API_COMPATIBILITY.md)
for a quick reference of exactly where (and why) this library's API shape differs from Apple's.

## Status

This project is under active development. Implemented so far:

- **Entity-component architecture** — `GKEntity`, `GKComponent`, `GKComponentSystem`
- **State machines** — `GKState`, `GKStateMachine`
- **Randomization** — `GKRandom`, `GKRandomSource` and its subclasses
  (`GKLinearCongruentialRandomSource`, `GKMersenneTwisterRandomSource`, `GKARC4RandomSource`),
  `GKRandomDistribution` and its subclasses (`GKGaussianDistribution`, `GKShuffledDistribution`)
- **Spatial partitioning** — `GKQuadtree`/`GKQuadtreeNode`, `GKOctree`/`GKOctreeNode`
- **Pathfinding** — `GKGraphNode`/`GKGraphNode2D`/`GKGraphNode3D`, `GKGraph`,
  `GKGridGraphNode`/`GKGridGraph`, `GKObstacle`/`GKCircleObstacle`/`GKPolygonObstacle`,
  `GKObstacleGraph`, `GKMeshGraph`
- **Agents, goals, and behaviors (steering)** — `GKAgent`/`GKAgent2D`/`GKAgent3D`,
  `GKAgentDelegate`, `GKPath`, `GKGoal`, `GKBehavior`
- **Rule systems** — `GKRule`, `GKRuleSystem`
- **Decision trees** — `GKDecisionNode`, `GKDecisionTree`
- **Game model AI (minmax / Monte Carlo)** — `GKGameModel`, `GKGameModelPlayer`,
  `GKGameModelUpdate`, `GKStrategist`, `GKMinmaxStrategist`, `GKMonteCarloStrategist`
- **Noise** — `GKNoise`, `GKNoiseMap`, and the `GKNoiseSource` hierarchy
  (`GKPerlinNoiseSource`, `GKRidgedNoiseSource`, `GKBillowNoiseSource`, `GKVoronoiNoiseSource`,
  `GKCheckerboardNoiseSource`, `GKCylindersNoiseSource`, `GKSpheresNoiseSource`,
  `GKConstantNoiseSource`)

A sample Android app is tracked separately in [`docs/ROADMAP.md`](docs/ROADMAP.md) (Phase 11).

## Usage

Each module below is usable independently — pick the ones your game needs. All classes live in the
`jp.co.bitz.gameplaykit` package. See each class's KDoc for the full API, and
[`docs/API_COMPATIBILITY.md`](docs/API_COMPATIBILITY.md) for how a given API deviates from Apple's.

### Entity-component architecture

```kotlin
class HealthComponent : GKComponent() {
    var health: Int = 100
}

val entity = GKEntity()
entity.addComponent(HealthComponent())
entity.component<HealthComponent>()?.health = 80
```

### State machines

```kotlin
class IdleState : GKState()

class WalkState : GKState() {
    override fun didEnter(previousState: GKState?) {
        println("started walking")
    }
}

val machine = GKStateMachine(listOf(IdleState(), WalkState()))
machine.enter<IdleState>()
machine.enter<WalkState>() // true — calls IdleState.willExit, then WalkState.didEnter
machine.update(deltaTime = 16.milliseconds)
```

### Randomization

```kotlin
val d6 = GKRandomDistribution.d6()
val roll = d6.nextInt() // 1..6

val source = GKMersenneTwisterRandomSource(seed = 42L)
val value = source.nextInt(upperBound = 100) // 0..99, reproducible for a given seed
```

### Spatial partitioning

```kotlin
val quadtree =
    GKQuadtree<String>(
        boundingQuad = GKQuad(min = Vector2(0f, 0f), max = Vector2(100f, 100f)),
        minimumCellSize = 4f,
    )
quadtree.add("player", point = Vector2(12f, 34f))
val nearby = quadtree.elements(GKQuad(min = Vector2(0f, 0f), max = Vector2(20f, 40f)))
```

### Pathfinding

```kotlin
val grid = GKGridGraph(gridOrigin = Vector2Int(0, 0), gridWidth = 10, gridHeight = 10, diagonalsAllowed = true)
val start = grid.node(Vector2Int(0, 0))!!
val goal = grid.node(Vector2Int(9, 9))!!
val path = grid.findPath(start, goal) // ordered list of GKGridGraphNode, empty if unreachable
```

### Agents, goals, and behaviors (steering)

```kotlin
val target = GKAgent2D().apply { position = Vector2(10f, 0f) }
val agent =
    GKAgent2D().apply {
        maxSpeed = 4f
        maxAcceleration = 8f
        behavior = GKBehavior.of(GKGoal.toSeekAgent(target), weight = 1f)
    }
agent.update(deltaTime = 16.milliseconds) // steers `agent` toward `target`
```

### Rule systems

```kotlin
val rules = GKRuleSystem()
rules.addRule(
    GKRule.fromPredicate(
        predicate = { it.getGrade("hungry") > 0.5f },
        action = { it.assertFact("shouldEat") },
    ),
)
rules.assertFact("hungry", grade = 0.8f)
rules.evaluate()
rules.getGrade("shouldEat") // 1f
```

### Decision trees

```kotlin
val tree = GKDecisionTree(attribute = "isHungry")
tree.rootNode.createBranch(value = true, attribute = "Eat")
tree.rootNode.createBranch(value = false, attribute = "Explore")
tree.findAction(answers = mapOf("isHungry" to true)) // "Eat"
```

### Game model AI (minmax / Monte Carlo)

```kotlin
// `game` implements GKGameModel for your own turn-based game's rules.
val strategist =
    GKMinmaxStrategist().apply {
        gameModel = game
        maxLookAheadDepth = 9
    }
val bestMove = strategist.bestMoveForActivePlayer() // a GKGameModelUpdate?, or null if no move exists
```

### Noise

```kotlin
val noise = GKNoise(GKPerlinNoiseSource(frequency = 1.0, octaveCount = 4, seed = 42))
val map = GKNoiseMap(noise, size = Vector2(1f, 1f), sampleCount = Vector2Int(256, 256))
val height = map.value(at = Vector2Int(10, 20)) // -1f..1f
```

## Requirements

- Android `minSdk` 24, `compileSdk`/`targetSdk` 34
- Kotlin 2.0+

## Building

```sh
./gradlew assemble          # build the library
./gradlew testDebugUnitTest # run unit tests
./gradlew ktlintCheck       # lint/format check
./gradlew detekt            # static analysis
```

See [`CLAUDE.md`](CLAUDE.md) for the full command reference and project structure.

## License

MIT — see [`LICENSE`](LICENSE).
