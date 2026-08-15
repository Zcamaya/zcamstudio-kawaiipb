# KawaiiPB Architecture

Last updated: 2026-08-15

## Overview

KawaiiPB is a feature-first Android app built with Kotlin and Jetpack Compose. It is organized around a kiosk session flow rather than a strict layered architecture, and the flow layer is the main area of complexity.

## Architectural style

The app currently follows a pragmatic hybrid structure:

- Compose-based UI
- ViewModel-driven state updates
- Domain models and use cases for app logic
- Repository abstraction for app configuration and dashboard data
- Storage and logging services for file and session IO

This is not a strict Clean Architecture app, but it is structured well enough for incremental refactoring without a full rewrite.

## Main project structure

```text
app/
  src/
    main/
      java/com/zcamstudio/kawaiipb/
        app/
        core/
        data/
        domain/
        feature/
        navigation/
        services/
      assets/
      res/
```

## Layer responsibilities

### App bootstrapping

- `KawaiiPbApp.kt` sets up dependencies and handles permission gating.
- `MainActivity.kt` hosts the Compose content.
- `KawaiiPbDependencies.kt` wires repository, storage, logging, and use case dependencies.

### Navigation

- `navigation/KawaiiNavHost.kt` manages route transitions between landing, flow, and admin screens.

### Feature layer

- `feature/landing/` manages entry and admin unlock behavior.
- `feature/flow/` owns the kiosk flow, capture, assignment, preview, and print process.
- `feature/admin/` owns admin configuration views and summary data.
- `feature/printing/` owns final print rendering and bitmap composition.

### Domain layer

- `domain/model/` contains app entities such as strip layouts, camera mode, template metadata, and kiosk state models.
- `domain/repository/` contains repository contracts.
- `domain/usecase/` contains app-level use cases.

### Data layer

- `data/repository/InMemoryKioskRepository.kt` provides the current concrete repository implementation.

### Services

- `services/storage/KawaiiStorageService.kt` handles local media, export paths, and public folder setup.
- `services/logging/SessionLogService.kt` records session activity for debugging and auditing.

### Shared UI

- `core/designsystem/` contains reusable Material3 design tokens and presentational helpers.
- `core/viewmodel/` contains shared ViewModel utilities when needed.

## Runtime flow

1. `MainActivity` launches the app.
2. `KawaiiPbApp` requests required media/storage permission if needed.
3. `KawaiiNavHost` starts at the landing screen.
4. User actions update the relevant ViewModel.
5. The ViewModel updates state and emits effects when screens need navigation.
6. Services provide storage, session logging, and rendering support.

## Main risk area

The `feature/flow` package is the most complex area in the codebase. It combines:

- capture timing
- photo assignment state
- strip layout selection
- template overlay selection
- sticker and transform editing
- print rendering
- session timer behavior

Because of this, the flow feature is the logical candidate for refactoring, but it is also the highest-risk surface for regressions.

## Documentation alignment

The JSON schema docs under `docs/json-schemas/` should remain aligned with the actual asset files in `app/src/main/assets/templates` and `app/src/main/assets/layouts`.

## Current recommendation

For the next refactor, keep the project architecture incremental:

- split stage-specific UI into smaller composables
- isolate pure state transitions from UI logic
- keep file and print IO in service boundaries
- preserve the current flow contracts until tests cover the behavior
