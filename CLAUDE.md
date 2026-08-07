# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Phase 0 (project scaffolding) is complete: a Gradle Android library project builds, tests, lints, and
has a `maven-publish` scaffold. Phase 1 (entity-component architecture), Phase 2 (state machines),
Phase 3 (randomization: `GKRandom`, `GKRandomSource` and its subclasses, `GKRandomDistribution`
and its subclasses), and Phase 4 (spatial partitioning: `GKQuadtree`/`GKQuadtreeNode`,
`GKOctree`/`GKOctreeNode`, plus the `Vector2`/`Vector3`/`GKQuad`/`GKBox` value types they're built on)
are implemented and tested. See `docs/ROADMAP.md` for full progress.

## Intent

This repo implements a **Kotlin library for Android that mirrors Apple's [GameplayKit](https://developer.apple.com/documentation/gameplaykit) API** — entity-component architecture, state machines, pathfinding (graphs/grids/obstacles), agents/goals/behaviors (steering), rule systems, randomization sources/distributions, decision trees, minmax/Monte Carlo game AI, spatial partitioning (quadtree/octree), and noise generation.

The goal is API and behavioral parity with Apple's GameplayKit types (`GKEntity`, `GKComponent`, `GKStateMachine`, `GKAgent`, `GKGraph`, etc.), but expressed in **idiomatic Kotlin** rather than a literal Obj-C/Swift transliteration (nullable types instead of Optional-as-comment, data/sealed classes, extension functions, no `NSPredicate` equivalent — `GKRule` uses Kotlin lambdas instead). Features tied to Apple-only rendering frameworks (SpriteKit/SceneKit scene binding, navmesh geometry) are out of scope since there's no Android equivalent to bind to. See `docs/ROADMAP.md`'s "Design Principle" section for details.

See `docs/ROADMAP.md` for the full implementation plan and progress checklist, organized by GameplayKit subsystem.

## Commands

All commands run from the repo root.

- Build: `./gradlew assemble`
- Unit tests: `./gradlew testDebugUnitTest` (a single test: `./gradlew testDebugUnitTest --tests "jp.co.bitz.gameplaykit.SomeTest"`)
- Lint/format check: `./gradlew ktlintCheck` (auto-fix: `./gradlew ktlintFormat`)
- Static analysis: `./gradlew detekt`
- Full CI-equivalent check: `./gradlew ktlintCheck detekt assemble testDebugUnitTest`
- Publish to local Maven repo (sanity-check publishing config): `./gradlew publishToMavenLocal`

If `ANDROID_HOME`/`ANDROID_SDK_ROOT` is not set in the shell, create a `local.properties` (gitignored)
with `sdk.dir=/path/to/Android/sdk`.

## Project structure

- `gameplaykit/` — the library module (`jp.co.bitz.gameplaykit`), namespace/group configured via
  `gradle.properties` (`GROUP`, `VERSION_NAME`) and `gameplaykit/build.gradle.kts`.
- `gradle/libs.versions.toml` — version catalog; add new dependencies/plugins here, not as
  hardcoded version strings in build files.
- `config/detekt/detekt.yml` — detekt rule overrides (builds upon detekt's default ruleset).

## Working in this repo

- **Documentation language:** all docs (README, KDoc, ROADMAP, etc.) must be written in **English**.
- **Documentation location:** project docs beyond the root `README.md` (roadmap, design notes, API compatibility notes, etc.) live under `docs/`.
- **`.gitignore`** covers macOS `.DS_Store` plus a standard Android/Gradle project (`.gradle/`, `build/`, `local.properties`, `*.apk`/`*.aab`, keystores, `google-services.json`, IntelliJ/Android Studio files).
- **Git operations:** do not run `git commit` or `git push` unless explicitly requested by the user for that specific change.
