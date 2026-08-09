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

- [ ] `GKDecisionNode` — tree node with attribute/weight
- [ ] `GKDecisionTree` — construction, evaluation, and pretty-printing

## Phase 9 — Game Model AI (Minmax / Monte Carlo)

- [ ] `GKGameModel` — protocol/interface for turn-based game state
- [ ] `GKGameModelPlayer` — protocol/interface for a player
- [ ] `GKGameModelUpdate` — protocol/interface for applying a move
- [ ] `GKStrategist` — common strategist contract
- [ ] `GKMinmaxStrategist` — minimax with alpha-beta pruning
- [ ] `GKMonteCarloStrategist` — Monte Carlo tree search

## Phase 10 — Noise (algorithmic core only)

- [ ] `GKNoise` / `GKNoiseMap` — sampled noise field
- [ ] `GKPerlinNoiseSource`
- [ ] `GKRidgedNoiseSource`
- [ ] `GKBillowNoiseSource`
- [ ] `GKVoronoiNoiseSource`
- [ ] `GKCheckerboardNoiseSource`
- [ ] `GKCylindersNoiseSource`
- [ ] `GKSpheresNoiseSource`
- [ ] `GKConstantNoiseSource`
- [ ] **Out of scope:** SceneKit terrain/geometry integration (no Android rendering equivalent)

## Phase 11 — Documentation & Samples

- [ ] KDoc for all public API surfaces
- [ ] README with usage examples per module
- [ ] Sample Android app demonstrating entities/components, pathfinding, and agents
- [ ] API compatibility notes (documented deviations from Apple's GameplayKit where Kotlin/Android
      constraints require a different shape)

## Explicitly Out of Scope

- `GKScene` / SpriteKit-SceneKit scene binding (no Android equivalent renderer)
- `GKMesh` / navmesh generation tied to SceneKit geometry
- Anything requiring Apple-only frameworks (Metal, SceneKit, SpriteKit) with no Android analog
