# Apple Parity Gap Audit & Remediation Plan

Audit date: 2026-11-15, against `develop` at `8a42a80` (0.2.0).

`docs/ROADMAP.md` (Phases 0–11) is complete, and `docs/API_COMPATIBILITY.md` records the
deviations decided on purpose as each phase landed. This document records what neither covers:
**Apple GameplayKit API that is absent from this port without having been written down as a
deviation.** It is the companion of the same audit in
[SpriteKit for Android](https://github.com/bitzgroup/SpriteKit) (`docs/PARITY_GAP_PLAN.md`),
which was triggered by a host app finding an unported API during Android TV work.

## How the audit was done, and its limits

- Every public declaration under `gameplaykit/src/main` was listed and compared, class by class,
  with Apple's documented GameplayKit API.
- The comparison is **from the source and from Apple's documentation only**. Nothing was
  re-verified against the real framework on a simulator, and no build or test was run as part of
  the audit. Confirm the Apple behavior on a real simulator/device before implementing an item.
- Items already in `docs/API_COMPATIBILITY.md` are not repeated, except in section 3.

Compared with SpriteKit, this library is close to complete: the gaps are a handful of classes and
members, not whole areas.

Priorities: **P1** small and commonly used · **P2** less common or needs design · **P3** large
or rarely used.

## 1. Missing API

### 1.1 Whole classes

| # | Missing | Notes | Pri |
| --- | --- | --- | --- |
| ~~G1~~ | ~~`GKCompositeBehavior`~~ | **Done**, verified on iOS/tvOS simulators against equivalent plain behaviors: member weights multiply their goals' weights, and nested composites multiply through. | P1 |
| ~~G2~~ | ~~`GKRTree`~~ | **Done**, verified on iOS/tvOS simulators: a query returns only elements its rect fully contains (not every one it intersects), and removal searches only child nodes whose bounds contain the rect passed. Result order after a split isn't Apple's (recorded deviation). | P2 |
| ~~G3~~ | ~~`GKSphereObstacle`~~ | **Done**, verified on iOS/tvOS simulators. Also found: Apple keeps 2D agents in 3D at (x, 0, y), so a 3D agent avoids a circle at (x, 0, y) and a 2D agent a sphere at (x, z). This library put circles at (x, y) as infinite cylinders for 3D agents; fixed. | P2 |

### 1.2 Missing members on existing classes

| # | Class | Missing | Pri |
| --- | --- | --- | --- |
| ~~G4~~ | ~~`GKObstacleGraph`~~ | ~~`lockConnection(from:to:)`, `unlockConnection(from:to:)`, `isConnectionLocked(from:to:)`; `connectUsingObstacles(node:ignoring:)`, `connectUsingObstacles(node:ignoringBufferRadiusOf:)`~~ — **Done**, verified on iOS/tvOS simulators: a lock keeps only its own direction. `connectNode` is renamed to Apple's `connectUsingObstacles`. | P1 |
| ~~G5~~ | ~~`GKGridGraph`~~ | ~~Generic custom node class (`GKGridGraph<NodeType>` / `classForGenericArgument(at:)`)~~ — **Done** as a `nodeFactory` parameter on `GKGridGraph`, `GKMeshGraph` and `GKObstacleGraph` (Apple's `nodeClass:`), verified on iOS/tvOS simulators. | P2 |
| ~~G6~~ | ~~`GKNoise`~~ | ~~`init(componentNoises:selectionNoise:)` and `init(componentNoises:selectionNoise:componentBoundaries:boundaryBlendDistances:)`~~ — **Done**, verified on iOS/tvOS simulators: the chosen component's `-1..1` range is squeezed into its band of the selection axis, and blend distances have no effect. Also found: Apple clamps every sampled value to `-1..1` (intermediate results stay unclamped); this library didn't, fixed. | P2 |
| ~~G7~~ | ~~`GKNoise`~~ | ~~`remapValues(toCurveWithControlPoints:)`, `remapValues(toTerracesWithPeaks:terracesInverted:)`~~ — **Done** as `remapValuesToCurve`/`remapValuesToTerraces`, following libnoise's curve/terrace modules: on iOS/tvOS simulators Apple's curve remap raises an exception for any dictionary and its terrace remap doesn't trace terraces (recorded deviation). | P2 |
| G8 | `GKPath` | 3D paths (`init(float3Points:…)`) — recorded as out of scope; needed only if `GKAgent3D` path following is wanted. | P3 |
| G9 | `GKEntity` / `GKComponent` | `copy()` — excluded with `NSCopying`; prototype-entity copying is a common pattern. Reconsider as explicit `open fun copy()`. | P3 |
| G10 | `GKAgent2D` / `GKAgent3D` | Found while probing G3: on Apple, setting `speed` sets the velocity along the agent's heading, a new 3D agent heads along +z, a new 2D agent's `rotation` is π/2, and `GKAgent3D.velocity` is read-only. Here `speed` is only a reading, both agents head along +x, and 3D velocity is settable. | P2 |

### 1.3 Behavior to verify — done

Verified on iOS and tvOS simulators. The results are recorded as tests in the tic-tac-toe
sample's `GameplayKitParity` target (`apple-platforms/GameplayKitParity/`) and mirrored in this
library's unit tests:

- ~~`GKRandomDistribution.nextInt(upperBound:)`~~ — **fixed.** Apple draws uniformly from
  `lowestValue..min(highestValue, upperBound - 1)` straight from the source, for every subclass,
  and raises if `upperBound` isn't above `lowestValue`. This library's `coerceAtMost` skewed toward
  the top value.
- ~~`GKShuffledDistribution` wrap-around~~ — **fixed.** Apple never repeats a value across the
  wrap-around; this library could.
- ~~`GKStateMachine` re-entering the current state / `update` with no state~~ — **match Apple.**
  Found on the way: Apple "succeeds" at entering an *unregistered* state and drops to a `nil`
  state. Kept as a recorded deviation (this library returns `false`).
- ~~`GKGridGraph.remove(_:)`~~ — **fixed.** Apple clears the lookup and cuts the removed nodes'
  connections (plain `GKGraph.remove` cuts connections too); `connectToAdjacentNodes` adds the node
  to the graph, and lookup never finds a node outside the grid. This library did none of these.

## 2. Proposed work order

Each step is one `feature/*` branch and PR against `develop`, updating
`API_COMPATIBILITY.md` in the same PR.

1. ~~Section 1.3 verification on a simulator; fix or document whatever differs.~~ Done.
2. ~~G1 (`GKCompositeBehavior`), G4 (`GKObstacleGraph` locking / ignore lists).~~ Done.
3. ~~G6, G7 (noise selection and remapping).~~ Done, plus the `-1..1` clamp it surfaced.
4. ~~G2 (`GKRTree`), G3 (`GKSphereObstacle`), G5 (custom grid/mesh node types).~~ Done, plus the
   obstacle placement G3 surfaced.
5. G10 (agent `speed`/default heading), found while doing step 4.
6. G8, G9 only on demand.

## 3. Suggested changes to recorded scope decisions — done

All three applied to `API_COMPATIBILITY.md`/`ROADMAP.md`.

- `GKScene` and SpriteKit integration are described as having "no Android equivalent renderer to
  bind to". That is no longer true: they live in
  [GKSKBridge](https://github.com/bitzgroup/GKSKBridge). Update the "Explicitly out of scope"
  section to point there instead.
- Move `remapValuesToCurve`/`remapValuesToTerraces` from "not implemented" to planned (G7).
- Add `GKRTree`, `GKCompositeBehavior`, `GKSphereObstacle` to `API_COMPATIBILITY.md` until they
  land, so their absence is recorded.

## 4. Keeping this from recurring

When a class is declared done, diff its public members against Apple's documentation page for
that class and record every omission in `API_COMPATIBILITY.md`. Update this file as items land;
delete it once section 1 is empty.
