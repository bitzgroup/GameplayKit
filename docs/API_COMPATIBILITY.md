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
- **No `copy()` on `GKEntity` or `GKComponent`.** Apple's `NSCopying` there is no basis for
  copying a prototype entity (verified on iOS/tvOS simulators, recorded in the tic-tac-toe sample's
  `GameplayKitParity` probes). An entity holding any component the game defines, or any entity
  subclass the game defines, copies to `nil`, which crashes a Swift caller. An entity made only of
  GameplayKit/SpriteKit classes copies as if archived: a `GKSKNodeComponent`'s node is duplicated,
  but an agent loses its position and speeds. A component copied alone is a new, unattached
  instance with none of the original's state. To copy entities, write a factory or a `copy()` of
  your own on your entity and component classes.

## State machines (`GKState`, `GKStateMachine`)

- **Entering an unregistered state fails here; Apple's "succeeds".** Apple's `canEnterState(_:)`
  and `enter(_:)` don't check that the machine holds the state class: both report `true`, and
  `enter` drops `currentState` to `nil`, passing `nil` to the current state's `willExit(to:)` —
  which crashes any Swift override of that non-optional parameter (verified on iOS/tvOS
  simulators, recorded in the tic-tac-toe sample's `GameplayKitParity` probes). This library
  returns `false` from both and leaves the machine unchanged instead.
- Verified to match Apple: re-entering the current state is a full transition (`willExit` then
  `didEnter`, both with the state itself, and `enter` returns `true` under the default
  `isValidNextState`); `update` with no current state does nothing.

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
- `GKRandomDistribution.nextInt(upperBound:)` matches Apple's undocumented behavior (verified on
  iOS/tvOS simulators, recorded in the tic-tac-toe sample's `GameplayKitParity` probes): a uniform
  draw from `lowestValue..min(highestValue, upperBound - 1)` straight from the random source, for
  every subclass — a Gaussian distribution is uniform here too, and a shuffled distribution's bag
  is left alone. An `upperBound` not above `lowestValue` throws `IllegalArgumentException` (Apple:
  `NSInvalidArgumentException`). (Before 0.3.0 it was `nextInt().coerceAtMost(upperBound - 1)`,
  which piled every excess draw onto the top value.)
- `GKShuffledDistribution` never draws the same value twice in a row: besides producing every value
  once per cycle, a new cycle never starts with the value the previous one ended on — Apple's
  behavior, verified the same way.

## Spatial partitioning (`GKQuadtree`, `GKOctree`, `GKRTree`)

- `GKRTree` matches Apple's observed behavior (verified on iOS/tvOS simulators, recorded in the
  tic-tac-toe sample's `GameplayKitParity` probes): a query returns only elements whose rect it
  fully contains, not every one it intersects; an element can be added more than once; removal
  looks only inside child nodes whose bounds contain the rect passed, and there removes the
  element's first record whatever its rect. Its methods are `add`/`remove`/`elements`, like
  `GKQuadtree`'s, rather than Apple's `addElement`/`removeElement`/`elements(inBoundingRectMin:)`.
  `maxNumberOfChildren` below 2 throws `IllegalArgumentException` (Apple's crashes on the second
  insert). *Result order is not Apple's* once the tree has split: the split strategies are the
  textbook algorithms (halving, Guttman's linear and quadratic, the R*-tree split), since Apple
  doesn't document its own; while every element fits in one node, results come in insertion
  order, as on Apple.
- All three relax Apple's `ElementType: AnyObject` constraint to Kotlin's `Any`, so value/data classes
  can be stored directly rather than requiring a reference-type wrapper — consistent with this
  library's idiomatic-Kotlin design principle.

## Pathfinding (`GKGraph`, `GKGridGraph`, `GKObstacleGraph`, `GKMeshGraph`)

- `GKGraphNode.pathFrom`/`pathTo` are renamed from GameplayKit's `findPath(from:)`/`findPath(to:)`,
  which differ only by Swift argument label and would collide as plain Kotlin overloads.
- `GKPolygonObstacle` exposes `vertices: List<Vector2>` directly rather than GameplayKit's paired
  `vertexCount`/`vertex(at:)` accessors.
- `GKObstacleGraph`'s buffer-radius vertex offsetting assumes convex, non-self-intersecting
  obstacle polygons (undocumented by Apple for the concave/self-intersecting case).
- `GKObstacleGraph.connectUsingObstacles(node, ignoring)` covers Apple's `connectUsingObstacles(node:)`
  and `connectUsingObstacles(node:ignoring:)`; `connectUsingObstacles(node:ignoringBufferRadiusOf:)`
  is `connectUsingObstaclesIgnoringBufferRadius`, since the two would clash as Kotlin overloads.
  (Before 0.4.0 the plain form was `connectNode`, still there as a deprecated alias.) Connection
  locks match Apple's observed behavior (verified on iOS/tvOS simulators): a lock is one-way,
  keeps only a connection that exists when obstacles change, and never creates one; adding or
  removing obstacles reconnects every node, those linked in from outside included.
- Apple's `nodeClass:` initializers for `GKGridGraph`, `GKMeshGraph` and `GKObstacleGraph` are a
  `nodeFactory` constructor parameter instead (pass a subclass's constructor, e.g. `::MyNode`),
  since Kotlin can't construct a type from a class object. The graphs aren't generic, so lookups
  return the base node type; cast back. As on Apple (verified on iOS/tvOS simulators), only the
  nodes a graph builds itself come from the factory: a plain node can still join a grid built of
  a subclass. (In Swift, the generic argument alone doesn't pick the class either — only
  `nodeClass:` does.) `classForGenericArgument(at:)` has no counterpart.
- `GKMeshGraph` triangulation uses the classic Bowyer-Watson algorithm (*contract-conformant, not
  bit-identical*: a valid Delaunay triangulation of the input points, not necessarily matching
  Apple's own triangulator's exact edge choices on degenerate/co-circular inputs).
- `GKGraph.remove` cuts every connection to or from the removed nodes, and `GKGridGraph.remove`
  also clears their position lookup; `GKGridGraph.connectNodeToAdjacentNodes` adds the node to the
  graph, and `node(position)` never finds a node outside the grid — all as Apple's does (verified
  on iOS/tvOS simulators, recorded in the tic-tac-toe sample's `GameplayKitParity` probes).
  Before 0.3.0, removal only unregistered the nodes, so pathfinding still routed through them.
- `GKPath`'s 3D variant is out of scope; only the 2D polygonal path used by follow/stay-on-path
  goals is implemented.

## Agents, goals, and behaviors (`GKAgent`, `GKGoal`, `GKBehavior`)

- `GKCompositeBehavior` sums its member behaviors' forces by weight, as Apple's does (verified on
  iOS/tvOS simulators). Unlike Apple, where an agent whose composite also has goals of its own
  crashes on update, those goals are simply added to the sum.
- `GKGoal.toAvoidObstacles` places obstacles as Apple does (verified on iOS/tvOS simulators):
  Apple keeps every agent in one 3D space where a 2D (x, y) is (x, 0, y), so a `GKAgent3D` avoids a
  `GKCircleObstacle` at (x, y) as the sphere at (x, 0, y), and a `GKAgent2D` avoids a
  `GKSphereObstacle` at (x, y, z) as the circle at (x, z). (Before 0.5.0, a 3D agent avoided a circle as
  an infinite cylinder along z, and spheres didn't exist.) A `GKPolygonObstacle` is avoided as its
  bounding circle.
- `GKAgent2D`/`GKAgent3D` share one internal `Vector3` position/velocity/heading simulation, so
  steering is implemented once for both dimensions rather than twice.
- Agent state matches Apple's observed behavior (verified on iOS/tvOS simulators): velocity is
  always the heading times `speed`, so setting `speed` or `rotation` moves it, and a negative
  `speed` points it backward; `speed` is capped at `maxSpeed` only on the next update. Defaults are
  Apple's — `maxSpeed` 1, `maxAcceleration` 1, `mass` 1, `radius` 0.5 — and a new `GKAgent2D` heads
  along +y (`rotation` π/2, read back in `0 until 2π`), a new `GKAgent3D` along +z. (Up to 0.4.0,
  `speed` was only a reading, every tunable defaulted to 0 so a default agent never moved, and
  both agents headed along +x.) Apple's `velocity` is read-only; the setters remain, deprecated,
  and set `rotation` and `speed`. A `mass` of 0 or less disables steering here; Apple's agent
  positions turn NaN.
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

- See "No `NSCopying`/`NSCoding` equivalents" above for `GKGameModel.copy()`, still required for
  API parity even though neither strategist calls it internally (see the next point).
- **`GKMinmaxStrategist` searches by mutating one shared model in place — `apply` a candidate move,
  recurse, `unapplyGameModelUpdate` it back off — matching Apple's own documented
  `GKMinmaxStrategist` behavior exactly, rather than branching by copying at every search node.**
  This was a deliberate reversal of this library's original design (which always branched by
  copying, treating `unapplyGameModelUpdate` as dead API-parity-only surface); revisited and
  changed after `bitzgroup/tic-tac-toe`'s iOS-first implementation order surfaced the concrete
  downside: an app's `GKGameModel` written and tested only against this port's original
  copy-branching behavior could ship with a broken `unapplyGameModelUpdate` that corrupts search on
  Apple's real framework, since only Apple's real `GKMinmaxStrategist` ever exercised it (confirmed
  by an on-device crash). Matching Apple's real strategy here means a `GKGameModel` correct against
  this library is now correct against Apple's real one too. **Consequence for `GKGameModel`
  implementations: `apply` and `unapplyGameModelUpdate` must be true inverses of each other for any
  model searched by `GKMinmaxStrategist`** — see `GKGameModel`'s own KDoc.
- **`GKMonteCarloStrategist` still searches by branching — calling `copy()` at every tree node —
  and does not require `unapplyGameModelUpdate` at all.** This was briefly unified with
  `GKMinmaxStrategist`'s mutate-and-backtrack approach too (for symmetry, on the assumption that
  the `GKMinmaxStrategist` finding above would generalize), then reverted once that assumption was
  checked directly: `bitzgroup/tic-tac-toe`'s `GKMonteCarloUnapplyCompatibilityTests` ran a
  `GKGameModel` with a no-op `unapplyGameModelUpdate` against Apple's real `GKMonteCarloStrategist`
  on-device (iOS Simulator, real `GameplayKit.framework`) and found it searches correctly, falling
  back to `copy(with:)`-based branching exactly as developer.apple.com's
  `unapplyGameModelUpdate(_:)` page documents for strategists in general — the behavior
  `GKMinmaxStrategist` turned out to be a documented exception to, not the norm. A `GKGameModel`
  only ever searched by `GKMonteCarloStrategist` can safely leave `unapplyGameModelUpdate` as the
  default no-op.
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
  Android equivalent wired into this library).
- **Sampled values are clamped to `-1..1`**, as on GameplayKit (verified on iOS/tvOS simulators):
  `GKNoise.value(at:)` and `GKNoiseMap` clamp what they return, while every operation works on
  unclamped values — a constant `3` plus a constant `-2.5` samples as `0.5`. (Before 0.5.0 nothing
  was clamped, so e.g. a constant `2.5` sampled as `2.5` instead of `1`.)
- **`GKNoise(componentNoises, selectionNoise, componentBoundaries, boundaryBlendDistances)`**
  matches GameplayKit's selection constructors (verified on iOS/tvOS simulators), including two
  undocumented behaviors: the chosen component's `-1..1` range is squeezed linearly into its band
  of the selection axis (two components with the default boundary `0` land in `-1..0` and `0..1`)
  rather than passed through, and `boundaryBlendDistances` have no effect. Default boundaries are
  evenly spaced; a selection value on a boundary picks the upper component. Where Apple traps on a
  wrong number of boundaries, this throws `IllegalArgumentException`.
- **`remapValuesToCurve(controlPoints)`/`remapValuesToTerraces(peaks, terracesInverted)`** instead
  of `remapValues(toCurveWithControlPoints:)`/`remapValues(toTerracesWithPeaks:terracesInverted:)`
  (Kotlin can't overload on labels alone), and *not* Apple's output: on iOS/tvOS simulators Apple's
  curve remap raises an `NSInvalidArgumentException` for any dictionary, and its terrace remap
  doesn't trace terraces (not monotonic, `terracesInverted` often ignored — recorded in the sample
  app's `GKNoiseParityTests`). Both follow the libnoise curve (cubic) and terrace (squared
  ease-in) modules GameplayKit's noise API is modeled on instead.
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

- **`GKScene` and SpriteKit integration** (`GKSKNodeComponent`, ...) — not in this library, which
  stays renderer-free; they live in [GKSKBridge](https://github.com/bitzgroup/GKSKBridge), which
  binds this library to [SpriteKit for Android](https://github.com/bitzgroup/SpriteKit).
- **Anything requiring Apple-only frameworks** (Metal, SceneKit, SpriteKit) with no Android analog.
- **A sample app in this repo.** The sample lives in its own repository,
  [bitzgroup/tic-tac-toe](https://github.com/bitzgroup/tic-tac-toe), which also records Apple's real GameplayKit behavior this library
  is checked against (its `GameplayKitParity` probes).

See `docs/ROADMAP.md` for the full phase-by-phase implementation history these notes are drawn from.
