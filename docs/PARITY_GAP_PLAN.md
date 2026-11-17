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
| G2 | `GKRTree` | The third spatial-partitioning structure beside `GKQuadtree`/`GKOctree`, with `GKRTreeSplitStrategy` (`halve`/`linear`/`quadratic`/`reduceOverlap`). Phase 4 covered only the other two and never recorded this one. | P2 |
| G3 | `GKSphereObstacle` | The 3D sibling of `GKCircleObstacle`, used by `GKGoal(toAvoid:maxPredictionTime:)` with `GKAgent3D`. Check that 3D agents can avoid obstacles at all today. | P2 |

### 1.2 Missing members on existing classes

| # | Class | Missing | Pri |
| --- | --- | --- | --- |
| ~~G4~~ | ~~`GKObstacleGraph`~~ | ~~`lockConnection(from:to:)`, `unlockConnection(from:to:)`, `isConnectionLocked(from:to:)`; `connectUsingObstacles(node:ignoring:)`, `connectUsingObstacles(node:ignoringBufferRadiusOf:)`~~ — **Done**, verified on iOS/tvOS simulators: a lock keeps only its own direction. `connectNode` is renamed to Apple's `connectUsingObstacles`. | P1 |
| G5 | `GKGridGraph` | Generic custom node class (`GKGridGraph<NodeType>` / `classForGenericArgument(at:)`) — the same limitation already recorded for `GKMeshGraph`, but not recorded here. A node-factory lambda parameter would cover both. | P2 |
| G6 | `GKNoise` | `init(componentNoises:selectionNoise:)` and `init(componentNoises:selectionNoise:componentBoundaries:boundaryBlendDistances:)` (noise selection/blending) | P2 |
| G7 | `GKNoise` | `remapValues(toCurveWithControlPoints:)`, `remapValues(toTerracesWithPeaks:terracesInverted:)` — recorded as "not implemented" on the grounds that they are rendering-adjacent; they are in fact pure value transforms and belong to the algorithmic core. | P2 |
| G8 | `GKPath` | 3D paths (`init(float3Points:…)`) — recorded as out of scope; needed only if `GKAgent3D` path following is wanted. | P3 |
| G9 | `GKEntity` / `GKComponent` | `copy()` — excluded with `NSCopying`; prototype-entity copying is a common pattern. Reconsider as explicit `open fun copy()`. | P3 |

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
3. G6, G7 (noise selection and remapping).
4. G2 (`GKRTree`), G3 (`GKSphereObstacle`), G5 (custom grid/mesh node types).
5. G8, G9 only on demand.

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
