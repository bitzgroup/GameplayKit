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

- [ ] `GKState` — base state class (`didEnter`, `willExit`, `update`, `isValidNextState`)
- [ ] `GKStateMachine` — state container, `enter`, `update`, `currentState`

## Phase 3 — Randomization

- [ ] `GKRandom` — common random source contract
- [ ] `GKRandomSource` — base random source (`nextInt`, `nextUniform`, `nextBool`)
- [ ] `GKLinearCongruentialRandomSource`
- [ ] `GKMersenneTwisterRandomSource`
- [ ] `GKARC4RandomSource`
- [ ] `GKRandomDistribution` (uniform distribution over a range)
- [ ] `GKGaussianDistribution`
- [ ] `GKShuffledDistribution`

## Phase 4 — Spatial Partitioning

- [ ] `GKQuadtree` / `GKQuadtreeNode` (2D spatial partitioning)
- [ ] `GKOctree` / `GKOctreeNode` (3D spatial partitioning)

## Phase 5 — Pathfinding

- [ ] `GKGraphNode` (base) / `GKGraphNode2D` / `GKGraphNode3D`
- [ ] `GKGraph` — node graph, `findPath`, `addNodes`, `removeNodes`, `connectToLowestCostNode`
- [ ] `GKGridGraphNode` / `GKGridGraph` (grid-based pathfinding, diagonal strategy)
- [ ] `GKObstacle` (base)
- [ ] `GKCircleObstacle`
- [ ] `GKPolygonObstacle`
- [ ] `GKObstacleGraph` (graph generated from obstacles, buffer radius)

## Phase 6 — Agents, Goals, and Behaviors (Steering)

- [ ] `GKAgent` (base) / `GKAgent2D` / `GKAgent3D`
- [ ] `GKAgentDelegate` — sync agent transform with owning object
- [ ] `GKGoal` — individual steering goals (seek, flee, avoid, separate, align, cohere, etc.)
- [ ] `GKBehavior` — weighted collection of goals

## Phase 7 — Rule Systems

- [ ] `GKRule` — base rule (predicate + action), Kotlin-lambda based (no `NSPredicate` equivalent)
- [ ] `GKRuleSystem` — ordered rule evaluation, fuzzy state via `grade`

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
