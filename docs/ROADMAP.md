# Implementation Roadmap

This document tracks progress implementing a Kotlin/Android library that mirrors Apple's
[GameplayKit](https://developer.apple.com/documentation/gameplaykit) API and behavior.

Check off items as they are implemented and tested. Items tied to Apple-platform-only concerns
(SpriteKit/SceneKit scene integration) are noted as **out of scope** since there is no Android
equivalent to bind to; the underlying algorithms are still implemented where useful standalone.

## Design Principle: Kotlin-Idiomatic, Not a Literal Port

The public API follows GameplayKit's design and behavior, but is expressed in idiomatic Kotlin
rather than a literal Obj-C/Swift-to-Kotlin transliteration. In particular:

- Nullability is modeled with Kotlin's type system (`?`), not Optionals-as-comments.
- Time intervals (Apple's `TimeInterval`, e.g. `update(deltaTime:)`) are modeled as
  `kotlin.time.Duration` rather than a raw `Double` of seconds, to prevent unit-confusion bugs.
- Prefer Kotlin constructs where they fit naturally: data classes, sealed classes/interfaces,
  extension functions, named/default arguments, property syntax over getter/setter methods.
- No `NSPredicate` equivalent — `GKRule` takes Kotlin lambdas instead.
- Class/member names follow GameplayKit naming (e.g. `GKEntity`, `GKStateMachine`) for
  discoverability by developers coming from Apple's docs, but internals and supporting APIs use
  standard Kotlin conventions (camelCase, no Hungarian/Obj-C prefixes on new types we introduce).
- Deviations from the Apple API shape are recorded as they happen (see Phase 11).

## Phase 0 — Project Setup

- [x] Scaffold Gradle Android library module (Kotlin DSL, `com.android.library` plugin)
- [x] Configure Kotlin, min/target/compile SDK versions (minSdk 24, compileSdk/targetSdk 34)
- [x] Configure unit test setup (JUnit / kotlin.test)
- [x] Configure `ktlint`/`detekt` (or chosen lint/format tooling)
- [x] Set up CI (build + test on push/PR via GitHub Actions)
- [x] Set up Maven publishing configuration (`maven-publish` scaffold, verified with
      `publishToMavenLocal`); actual Maven Central/JitPack release credentials still TBD

## Phase 1 — Entity-Component Architecture

- [x] `GKEntity` — container holding a set of components
- [x] `GKComponent` — base component class, `updateTime`
- [x] `GKComponentSystem` — homogeneous component collection with ordered `update`

## Phase 2 — State Machines

- [x] `GKState` — base state class (`didEnter`, `willExit`, `update`, `isValidNextState`)
- [x] `GKStateMachine` — state container, `enter`, `update`, `currentState`

## Phase 3 — Randomization

- [x] `GKRandom` — common random source contract
- [x] `GKRandomSource` — base random source (`nextInt`, `nextUniform`, `nextBool`)
- [x] `GKLinearCongruentialRandomSource` — backed directly by `java.util.Random`, which
      GameplayKit's docs confirm implements the same 48-bit LCG algorithm (bit-for-bit verified)
- [x] `GKMersenneTwisterRandomSource` — reference MT19937-64 core generator (matches
      `std::mt19937_64`); nextInt/nextUniform derivation is not documented by Apple, so exact
      output is not guaranteed bit-identical to GameplayKit, only the same core algorithm
- [x] `GKARC4RandomSource` — RC4 keystream (KSA + PRGA) with `dropValues` support
- [x] `GKRandomDistribution` (uniform distribution over a range)
- [x] `GKGaussianDistribution` — Marsaglia polar method; matches the documented mean/deviation
      contract, not bit-identical to Apple's undocumented internal sampler
- [x] `GKShuffledDistribution`

## Phase 4 — Spatial Partitioning

- [x] `Vector2` / `Vector3` — stand-ins for GameplayKit's `vector_float2`/`vector_float3` SIMD types,
      since there is no SIMD vector type in the Kotlin/Android standard library
- [x] `GKQuad` / `GKBox` — axis-aligned 2D/3D bounding regions
- [x] `GKQuadtree` / `GKQuadtreeNode` (2D spatial partitioning)
- [x] `GKOctree` / `GKOctreeNode` (3D spatial partitioning)

## Phase 5 — Pathfinding

- [x] `Vector2Int` — stand-in for GameplayKit's `vector_int2` SIMD type, used for grid coordinates
- [x] `GKGraphNode` (base) / `GKGraphNode2D` / `GKGraphNode3D` — A* pathfinding via `pathFrom`/`pathTo`
      (renamed from GameplayKit's `findPath(from:)`/`findPath(to:)`, which differ only by Swift
      argument label and would collide as plain Kotlin overloads)
- [x] `GKGraph` — node graph, `findPath`, `add`/`remove` nodes, `connectToLowestCostNode`
- [x] `GKGridGraphNode` / `GKGridGraph` (grid-based pathfinding, `diagonalsAllowed`)
- [x] `GKObstacle` (base)
- [x] `GKCircleObstacle`
- [x] `GKPolygonObstacle` — exposes `vertices: List<Vector2>` rather than GameplayKit's paired
      `vertexCount`/`vertex(at:)` accessors
- [x] `GKObstacleGraph` (visibility graph generated from obstacles, buffer radius); vertex-offset
      buffering assumes convex, non-self-intersecting obstacle polygons
- [x] `GKMeshGraph` / `GKTriangle` / `GKMeshGraphTriangulationMode` — Delaunay-triangulated navmesh
      graph generated from obstacles (an alternative to `GKObstacleGraph`, added after initially
      being miscategorized as SceneKit-only — the real `GKMeshGraph` is purely 2D/obstacle-based,
      just like `GKObstacleGraph`). Not generic over a custom node subclass (Apple's
      `GKMeshGraph<NodeType>` supports one via Obj-C's dynamic class-based construction); triangulation
      uses the classic Bowyer-Watson algorithm since GameplayKit doesn't document its own

## Phase 6 — Agents, Goals, and Behaviors (Steering)

- [x] `GKAgent` (base, a `GKComponent`) / `GKAgent2D` / `GKAgent3D` — share one internal `Vector3`
      position/velocity/heading simulation, so steering is implemented once for both dimensions;
      `GKAgent3D.rotation` is a normalized forward-direction `Vector3` rather than GameplayKit's
      `simd_quatf` (no other API in this port needs roll around the forward axis)
- [x] `GKAgentDelegate` — `agentWillUpdate`/`agentDidUpdate`, sync agent transform with owning object
- [x] `GKPath` — 2D-only polygonal path (`points`, `radius`, `cyclical`) used by follow/stay-on-path
      goals; GameplayKit's 3D path variant is out of scope
- [x] `GKGoal` — seek, flee, avoidAgents, avoidObstacles, separate, align, cohere, reachTargetSpeed,
      interceptAgent, followPath, stayOnPath, wander. Implemented as standard Reynolds
      steering-behavior formulas (GameplayKit's own internal implementation is undocumented, so this
      is contract-conformant, not bit-identical) wrapped in Kotlin lambdas rather than an opaque
      native goal-type enum (consistent with `GKRule`'s use of lambdas)
- [x] `GKBehavior` — weighted collection of goals

## Phase 7 — Rule Systems

- [x] `GKRule` — base rule (`salience`, open `evaluatePredicate`/`performAction` for subclassing);
      companion factories `fromPredicate`/`toAssertFact`/`toRetractFact` take Kotlin lambdas instead
      of GameplayKit's `NSPredicate`-based `GKRule.fromPredicateAssertingFact` family, which is not
      implemented here
- [x] `GKRuleSystem` — `agenda`/`executed`/`rules`/`state`, `addRule(s)`/`removeAllRules`,
      `evaluate()` (ascending-salience order, ties broken by insertion order — inferred from Apple's
      own FizzBuzz `GKRuleSystem` example, since the prose alone doesn't specify tie-break direction),
      `reset()`; fuzzy state via `assertFact`/`retractFact`/`getGrade` (grade accumulated/diminished,
      clamped to `[0, 1]`) and `getMinimumGrade`/`getMaximumGrade` (fuzzy AND/OR over facts)

## Phase 8 — Decision Trees

- [x] `GKDecisionNode` — tree node built via `createBranch(value:attribute:)`/`createBranch(predicate:attribute:)`
      (Kotlin lambda instead of `NSPredicate`)/`createBranch(weight:attribute:)`; `attribute` is
      exposed as a public, readable property (Apple keeps it private — there is no other way to
      introspect a Kotlin-built tree) and there is no public constructor, matching GameplayKit
- [x] `GKDecisionTree` — `init(attribute:)`, `findAction`, `randomSource` (used to resolve weighted
      branches); the `examples`/`actions`/`attributes` constructor builds a tree by the classic ID3
      algorithm (highest information gain first) — GameplayKit doesn't document its own tie-break/
      pruning behavior, so this is contract-conformant (fits the training data), not bit-identical;
      `prettyPrint()` is an addition beyond GameplayKit's API (Apple exposes no tree introspection
      at all) for debugging trees built in Kotlin. Not implemented: the `NSCoder`/`NSURL`-based
      initializers and `export(to:)` — this library has no `NSCoding`/archiving equivalent anywhere

## Phase 9 — Game Model AI (Minmax / Monte Carlo)

- [x] `GKGameModel` — interface for turn-based game state (`players`, `activePlayer`, `copy`,
      `setGameModel`, `gameModelUpdates`, `apply`; optional `score`/`isWin`/`isLoss`/
      `unapplyGameModelUpdate` default to `0`/`false`/`false`/no-op). `copy()` stands in for
      GameplayKit's `NSCopying` conformance (no Kotlin equivalent) and must be a true deep copy:
      both strategists below branch their search by copying rather than by mutating one shared
      model via `apply`/`unapplyGameModelUpdate`, so `unapplyGameModelUpdate` is kept only for API
      parity and is never called internally — GameplayKit documents its own `GKMinmaxStrategist`
      as backtracking via unapply for space efficiency, which this port doesn't attempt to match
- [x] `GKGameModelPlayer` — interface for a player (`playerId`)
- [x] `GKGameModelUpdate` — interface for a move (`value`)
- [x] `GKStrategist` — common strategist contract (`gameModel`, `randomSource`,
      `bestMoveForActivePlayer`)
- [x] `GKMinmaxStrategist` — minimax with alpha-beta pruning, generalized past two players by
      treating every player other than the one being searched for as a single adversary; also
      `bestMove(for:)` (any player, not just the active one) and `randomMove(for:numMovesToConsider:)`.
      Tie-breaking and exact bit-identical move choice aren't documented by Apple, so this keeps
      the first-seen move on a tie
- [x] `GKMonteCarloStrategist` — Monte Carlo tree search with the standard UCT selection rule;
      `budget`/`explorationParameter` per GameplayKit, plus a `maxPlayoutDepth` rollout safety cap
      this port adds since GameplayKit doesn't document its own rollout termination policy either

## Phase 10 — Noise (algorithmic core only)

- [x] `GKNoise` — wraps a `GKNoiseSource` and composes it via `add`/`multiply`/`getMaximum`/
      `getMinimum`/`raiseToPower`/`clamp`/`applyAbsoluteValue`/`invert`/`applyTurbulence`/
      `displace`/`move`/`scale`/`rotate` (each mutates the instance in place, mirroring
      GameplayKit's own "builder via mutation" GKNoise API) and sampled via `value(at:)`.
      Deviation: positions are this library's existing single-precision `Vector3` rather than
      GameplayKit's double-precision `vector_double3` — not worth a parallel vector type for one
      API. Not implemented: the `gradientColors`-based constructor/property (`UIColor`/`NSColor`,
      no Android equivalent) and `remapValuesToCurve`/`remapValuesToTerraces` (texture/curve
      editing conveniences, rendering-adjacent rather than algorithmic core)
- [x] `GKNoiseMap` — slices a finite 2D grid (`size`/`origin`/`sampleCount`) out of a `GKNoise`
      field, `value(at:)`/`setValue(_:at:)`/`interpolatedValue(at:)` (bilinear). `seamless` wraps
      grid coordinates modulo `sampleCount` (a simple tileable approximation — GameplayKit doesn't
      document its own seamless-blending algorithm)
- [x] `GKNoiseSource` (abstract base, no public sampling API of its own — matches GameplayKit) /
      `GKCoherentNoiseSource` (abstract; `frequency`/`octaveCount`/`lacunarity`/`seed`, each
      subclass sums `octaveCount` octaves of a shared Perlin core — Ken Perlin's reference
      "improved noise" permutation-table algorithm, since GameplayKit doesn't document its own
      noise internals)
- [x] `GKPerlinNoiseSource` — classic fractal-sum Perlin noise, `persistence` per octave
- [x] `GKRidgedNoiseSource` — `(1 - |noise|)^2` per octave (sharp ridges); GameplayKit doesn't
      document its own ridged-multifractal formula (real libnoise-style implementations also apply
      a spectral weighting/gain step this omits), so this is contract-conformant, not bit-identical
- [x] `GKBillowNoiseSource` — `|noise| * 2 - 1` per octave (rounded features), `persistence`
- [x] `GKVoronoiNoiseSource` — classic cell/feature-point (Worley) noise; `frequency`/
      `displacement`/`distanceEnabled`/`seed`
- [x] `GKCheckerboardNoiseSource` — `squareSize`
- [x] `GKCylindersNoiseSource` — `frequency`
- [x] `GKSpheresNoiseSource` — `frequency`
- [x] `GKConstantNoiseSource` — `value`
- [x] **Out of scope:** SceneKit terrain/geometry integration (no Android rendering equivalent)

## Phase 11 — Documentation & Samples

- [x] KDoc for all public API surfaces
- [ ] API compatibility notes (documented deviations from Apple's GameplayKit where Kotlin/Android
      constraints require a different shape)
- [ ] README with usage examples per module
- **Not in this repo:** a sample Android app demonstrating entities/components, pathfinding, and
      agents is planned, but will live in its own repository rather than in-tree here (decided
      separately from this checklist)

## Explicitly Out of Scope

- `GKScene` — associates GameplayKit entities/graphs with a SpriteKit `SKScene` (or SceneKit
  scene); no Android equivalent renderer to bind to
- (Previously listed here as "`GKMesh` / navmesh generation tied to SceneKit geometry" — that was
  a misidentification. The real class is `GKMeshGraph`, and it's purely 2D/obstacle-based like
  `GKObstacleGraph`, with no SceneKit dependency; it's implemented in Phase 5.)
- Anything requiring Apple-only frameworks (Metal, SceneKit, SpriteKit) with no Android analog
