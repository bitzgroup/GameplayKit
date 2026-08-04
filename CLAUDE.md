# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repository is a **new, unstarted project**. It currently contains only a `README.md`, `.gitignore`, and `docs/ROADMAP.md`; there is no source code or build configuration yet.

## Intent

This repo implements a **Kotlin library for Android that mirrors Apple's [GameplayKit](https://developer.apple.com/documentation/gameplaykit) API** — entity-component architecture, state machines, pathfinding (graphs/grids/obstacles), agents/goals/behaviors (steering), rule systems, randomization sources/distributions, decision trees, minmax/Monte Carlo game AI, spatial partitioning (quadtree/octree), and noise generation.

The goal is API and behavioral parity with Apple's GameplayKit types (`GKEntity`, `GKComponent`, `GKStateMachine`, `GKAgent`, `GKGraph`, etc.), adapted to idiomatic Kotlin naming/conventions where a direct Obj-C/Swift mapping doesn't make sense (e.g. no `NSPredicate` equivalent — `GKRule` uses Kotlin lambdas instead). Features tied to Apple-only rendering frameworks (SpriteKit/SceneKit scene binding, navmesh geometry) are out of scope since there's no Android equivalent to bind to.

See `docs/ROADMAP.md` for the full implementation plan and progress checklist, organized by GameplayKit subsystem.

## Working in this repo

- **Documentation language:** all docs (README, KDoc, ROADMAP, etc.) must be written in **English**.
- **Documentation location:** project docs beyond the root `README.md` (roadmap, design notes, API compatibility notes, etc.) live under `docs/`.
- **`.gitignore`** covers macOS `.DS_Store` plus a standard Android/Gradle project (`.gradle/`, `build/`, `local.properties`, `*.apk`/`*.aab`, keystores, `google-services.json`, IntelliJ/Android Studio files). Expect the project to be scaffolded as a Gradle-based Android library (see ROADMAP.md Phase 0).
- No build, lint, or test commands exist yet — do not assume Gradle wrapper scripts, module layout, or package names until they are actually created. When scaffolding the project, follow standard Android library conventions (Gradle Kotlin DSL, `src/main/java|kotlin`, `src/test`, `src/androidTest`) unless the user specifies otherwise.
- **Git operations:** do not run `git commit` or `git push` unless explicitly requested by the user for that specific change.
