# GameplayKit

A Kotlin library for Android that implements the same API as Apple's
[GameplayKit](https://developer.apple.com/documentation/gameplaykit) framework — entity-component
architecture, state machines, pathfinding, agent steering behaviors, rule systems, randomization,
decision trees, minmax/Monte Carlo game AI, spatial partitioning, and noise generation.

The goal is API and behavioral parity with GameplayKit's types (`GKEntity`, `GKStateMachine`,
`GKAgent`, `GKGraph`, and so on), expressed in idiomatic Kotlin rather than a literal port. See
[`docs/ROADMAP.md`](docs/ROADMAP.md) for the design principles behind that adaptation and the full
implementation progress checklist.

## Status

This project is under active development. Implemented so far:

- **Entity-component architecture** — `GKEntity`, `GKComponent`, `GKComponentSystem`
- **State machines** — `GKState`, `GKStateMachine`
- **Randomization** — `GKRandom`, `GKRandomSource` and its subclasses
  (`GKLinearCongruentialRandomSource`, `GKMersenneTwisterRandomSource`, `GKARC4RandomSource`),
  `GKRandomDistribution` and its subclasses (`GKGaussianDistribution`, `GKShuffledDistribution`)

Everything else — pathfinding, agents/goals/behaviors, rule systems, decision trees, minmax/Monte
Carlo strategists, spatial partitioning, and noise — is tracked in
[`docs/ROADMAP.md`](docs/ROADMAP.md).

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
