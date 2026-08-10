# API Compatibility Notes

This library mirrors Apple's [GameplayKit](https://developer.apple.com/documentation/gameplaykit)
API and behavior, but is written in idiomatic Kotlin rather than a literal Obj-C/Swift-to-Kotlin
transliteration (see `docs/ROADMAP.md`'s "Design Principle" section). This document is a quick
reference, organized by subsystem, for developers who already know Apple's GameplayKit and want to
know exactly where — and why — this library's shape differs. It does not restate behavior that
matches Apple's docs; only intentional deviations, omissions, and additions are listed.

Two deviation categories recur throughout and are called out once here rather than per item below:

- **Contract-conformant, not bit-identical.** Several GameplayKit algorithms (noise internals, the
  Gaussian sampler, Monte Carlo/minimax tie-breaking, ID3 pruning, ridged/billow noise formulas,
  navmesh triangulation) are not documented by Apple beyond their observable contract. Where noted,
  this library implements a standard/reference algorithm that satisfies the same contract, but its
  output is not guaranteed to match Apple's internal implementation bit-for-bit.
- **No Foundation/Obj-C runtime equivalents.** Kotlin has no `NSPredicate`, `NSCopying`, `NSCoding`,
  or SIMD vector types. Every place GameplayKit's API surface depends on one of these is called out
  below with its Kotlin replacement.

## General conventions

- **Nullability** uses Kotlin's `?` type system throughout, not Optional-as-comment.
- **Time intervals** (`update(deltaTime:)`, Apple's `TimeInterval`) are `kotlin.time.Duration`
  rather than a raw `Double` of seconds, to prevent unit-confusion bugs.
- **No `NSPredicate` equivalent.** Every GameplayKit API that takes an `NSPredicate` takes a plain
  Kotlin lambda instead: `GKRule.fromPredicate`, `GKDecisionNode.createBranch(predicate:attribute:)`.
  The `NSPredicate`-based `GKRule.fromPredicateAssertingFact` family and `GKNSPredicateRule` are not
  implemented.
- **No `NSCopying`/`NSCoding` equivalents.** `GKGameModel.copy()` is this library's stand-in for
  `NSCopying` and must return a true deep copy. `GKDecisionTree`'s `NSCoder`/`NSURL`-based
  initializers and `export(to:)` are not implemented — this library has no archiving/persistence
  layer anywhere, so tree (and any other) persistence is left to the host application.
- **No SIMD vector types.** `Vector2`/`Vector3` (single-precision) and `Vector2Int` stand in for
  `vector_float2`/`vector_float3`/`vector_int2`. `GKNoise` samples single-precision `Vector3`
  positions rather than GameplayKit's double-precision `vector_double3` — not worth a parallel
  vector type for one API.
- **Property syntax over getter/setter methods**, named/default arguments, data/sealed classes, and
  extension functions are used where they fit naturally; class/member names still follow
  GameplayKit's naming (`GKEntity`, `GKStateMachine`, ...) for discoverability.

## Entity-component architecture (`GKEntity`, `GKComponent`, `GKComponentSystem`)

- Typed component lookup (`GKEntity.component<T>()`, `GKComponentSystem<T>()`) uses reified
  generics instead of Obj-C's `Class` parameter; lookup matches the *exact* runtime class, same as
  GameplayKit (a subclass component is not found by its superclass's type).

## State machines (`GKState`, `GKStateMachine`)

- No deviations beyond the general conventions above.

## Randomization (`GKRandom`, `GKRandomSource`, `GKRandomDistribution`)

- `GKLinearCongruentialRandomSource` delegates directly to `java.util.Random`, which Apple's own
  docs confirm implements the identical 48-bit LCG algorithm — verified bit-for-bit against
  `java.util.Random` in this library's test suite.
- `GKMersenneTwisterRandomSource` implements the reference MT19937-64 core generator (matching
  `std::mt19937_64`). *Contract-conformant, not bit-identical*: `nextInt`/`nextUniform`'s exact
  bit-derivation from the 64-bit words isn't documented by Apple.
- `GKGaussianDistribution` uses the Marsaglia polar method. *Contract-conformant, not
  bit-identical*: it satisfies the documented mean/deviation contract, not Apple's undocumented
  internal sampler.
- `GKRandomDistribution.nextInt(upperBound:)`'s combination with a distribution's own range isn't
  documented by Apple; this library defines it uniformly as `nextInt().coerceAtMost(upperBound - 1)`
  across the base class and every subclass.

## Spatial partitioning (`GKQuadtree`, `GKOctree`)

- No deviations beyond the general conventions above.

## Pathfinding (`GKGraph`, `GKGridGraph`, `GKObstacleGraph`, `GKMeshGraph`)

- `GKGraphNode.pathFrom`/`pathTo` are renamed from GameplayKit's `findPath(from:)`/`findPath(to:)`,
  which differ only by Swift argument label and would collide as plain Kotlin overloads.
- `GKPolygonObstacle` exposes `vertices: List<Vector2>` directly rather than GameplayKit's paired
  `vertexCount`/`vertex(at:)` accessors.
- `GKObstacleGraph`'s buffer-radius vertex offsetting assumes convex, non-self-intersecting
  obstacle polygons (undocumented by Apple for the concave/self-intersecting case).
- `GKMeshGraph` is not generic over a custom node subclass — GameplayKit's `GKMeshGraph<NodeType>`
  supports one via Obj-C's dynamic class-based construction, which Kotlin has no equivalent for.
  Triangulation uses the classic Bowyer-Watson algorithm (*contract-conformant, not
  bit-identical*: a valid Delaunay triangulation of the input points, not necessarily matching
  Apple's own triangulator's exact edge choices on degenerate/co-circular inputs).
- `GKPath`'s 3D variant is out of scope; only the 2D polygonal path used by follow/stay-on-path
  goals is implemented.

## Agents, goals, and behaviors (`GKAgent`, `GKGoal`, `GKBehavior`)

- `GKAgent2D`/`GKAgent3D` share one internal `Vector3` position/velocity/heading simulation, so
  steering is implemented once for both dimensions rather than twice.
- `GKAgent3D.rotation` is a normalized forward-direction `Vector3`, not GameplayKit's `simd_quatf`
  — no other API in this port needs roll around the forward axis, so a full quaternion type wasn't
  judged worth introducing for this one property.
- `GKGoal`'s steering behaviors (seek, flee, avoidAgents, ...) are standard Reynolds
  steering-behavior formulas. *Contract-conformant, not bit-identical*: GameplayKit's own internal
  implementation is undocumented. Goals are Kotlin lambdas under the hood rather than an opaque
  native goal-type enum, consistent with `GKRule`'s lambda-based design.

## Rule systems (`GKRule`, `GKRuleSystem`)

- See "No `NSPredicate` equivalent" above for `GKRule.fromPredicate`/`toAssertFact`/`toRetractFact`.
- `GKRuleSystem.evaluate()`'s tie-break order (ascending salience, ties broken by insertion order)
  is inferred from Apple's own FizzBuzz `GKRuleSystem` example, since the prose documentation alone
  doesn't specify tie-break direction.

## Decision trees (`GKDecisionNode`, `GKDecisionTree`)

- `GKDecisionNode.attribute` is a public, readable property. Apple's real `GKDecisionNode` keeps it
  private — the only public surface is building (`createBranch`) and querying
  (`GKDecisionTree.findAction`) — but a Kotlin developer has no other way to introspect or debug a
  tree they've built, so it's exposed here.
- `GKDecisionTree.prettyPrint()` is an addition beyond GameplayKit's API (Apple exposes no tree
  introspection at all), added for debugging trees built in Kotlin.
- The `examples`/`actions`/`attributes` constructor builds a tree via the classic ID3 algorithm
  (highest information gain first). *Contract-conformant, not bit-identical*: GameplayKit doesn't
  document its own tie-break/pruning behavior.
- See "No `NSCopying`/`NSCoding` equivalents" above for the unimplemented `NSCoder`/`NSURL`-based
  initializers and `export(to:)`.

## Game model AI (`GKGameModel`, `GKMinmaxStrategist`, `GKMonteCarloStrategist`)

- See "No `NSCopying`/`NSCoding` equivalents" above for `GKGameModel.copy()`. Both strategists
  always branch the search tree by copying rather than by mutating one shared model via
  `apply`/`unapplyGameModelUpdate`; `unapplyGameModelUpdate` is kept only for API parity and is
  never called internally. Apple documents `GKMinmaxStrategist`'s own implementation as
  backtracking via unapply for space efficiency, which this port does not attempt to replicate.
- `GKMinmaxStrategist.bestMove(player:)` (any player, not just the active one) and
  `randomMove(player:numMovesToConsider:)` are implemented as documented, but exact tie-break/move
  choice on equal scores isn't specified by Apple; this implementation keeps the first-seen move.
- `GKMonteCarloStrategist`'s UCT exploration constant, rollout policy, and tie-break rule aren't
  documented by Apple, so its search is *contract-conformant, not bit-identical*: it approaches the
  optimal move as `budget` grows. `maxPlayoutDepth` is an addition beyond GameplayKit's API — a
  rollout safety cap for game models with no guaranteed terminal state within a given horizon, since
  Apple documents no such cap either (its own rollout termination policy isn't public).

## Noise (`GKNoise`, `GKNoiseMap`, `GKNoiseSource` and subclasses)

- `GKNoise` samples single-precision `Vector3` positions; see "No SIMD vector types" above.
- Not implemented: the `gradientColors`-based constructor/property (tied to `UIColor`/`NSColor`, no
  Android equivalent wired into this library) and `remapValuesToCurve`/`remapValuesToTerraces`
  (texture/curve-editing conveniences, rendering-adjacent rather than algorithmic core — this
  library implements noise's algorithmic core only, per `docs/ROADMAP.md`'s Phase 10 scope).
- `GKNoise.rotate`'s Euler rotation order (X then Y then Z) is *contract-conformant, not
  bit-identical*: GameplayKit doesn't document its own rotation order.
- `GKNoiseMap.seamless` wraps grid coordinates modulo `sampleCount`, a simple tileable
  approximation. *Contract-conformant, not bit-identical*: GameplayKit doesn't document its own
  seamless-blending algorithm.
- `GKRidgedNoiseSource`/`GKBillowNoiseSource` implement the classic ridged/billow transforms over a
  Perlin core. *Contract-conformant, not bit-identical*: Apple doesn't document its own
  ridged-multifractal formula (real libnoise-style implementations also apply a spectral
  weighting/gain step this omits).
- `GKNoiseSource` exposes no public sampling API of its own, matching GameplayKit — only `GKNoise`
  samples a source, via `GKNoise.value(at:)`. Subclassing it isn't meaningfully supported by
  Apple's public API either (there's no documented override point), so this library's `sample`
  extension point is `internal` rather than public.

## Explicitly out of scope

- **`GKScene`** — associates GameplayKit entities/graphs with a SpriteKit `SKScene` (or SceneKit
  scene); there is no Android equivalent renderer to bind to.
- **Anything requiring Apple-only frameworks** (Metal, SceneKit, SpriteKit) with no Android analog.
- A sample Android app demonstrating entities/components, pathfinding, and agents is planned, but
  will live in its own repository rather than in-tree here.

See `docs/ROADMAP.md` for the full phase-by-phase implementation history these notes are drawn from.
