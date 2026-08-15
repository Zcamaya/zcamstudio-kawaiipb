# KawaiiPB System Structure

Last updated: 2026-08-15

## Purpose

KawaiiPB is a Compose-based Android kiosk app for guided photo booth sessions. It currently combines camera capture, assignment, template handling, preview, printing, and admin configuration in a single app flow.

## Top-level layout

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
docs/
  json-schemas/
```

## Main subsystems

### App bootstrap

- `MainActivity.kt` hosts the Compose app.
- `KawaiiPbApp.kt` handles permission gating and dependency wiring.
- `app/KawaiiPbDependencies.kt` provides the shared application dependencies to screens and ViewModels.

### Navigation

- `navigation/KawaiiNavHost.kt` controls the landing, flow, and admin routes.

### Feature layer

- `feature/landing/` handles landing and admin unlock flow.
- `feature/flow/` contains the kiosk flow state and UI.
- `feature/admin/` contains admin configuration screens.
- `feature/printing/` generates print output and render assets.

### Domain layer

- `domain/model/` contains shared models.
- `domain/repository/` defines repository contracts.
- `domain/usecase/` defines app use cases.

### Data layer

- `data/repository/` contains the concrete in-memory repository implementation.

### Services

- `services/storage/` handles file access, export destinations, and persistent settings.
- `services/logging/` records session events.

### Shared UI

- `core/designsystem/` contains reusable Compose UI and theme building blocks.
- `core/viewmodel/` contains viewmodel utilities when needed.

## Relevant flow files

The central flow implementation is currently spread across these files:

- `feature/flow/presentation/FlowScreen.kt` - screen coordinator
- `feature/flow/presentation/FlowUiState.kt` - state model
- `feature/flow/presentation/FlowViewModel.kt` - flow logic and transitions
- `feature/flow/presentation/FlowStageSections.kt` - stage-specific UI
- `feature/flow/presentation/FlowCaptureSections.kt` - capture UI
- `feature/flow/presentation/FlowAssignmentSections.kt` - assignment flow
- `feature/flow/presentation/FlowSharedSections.kt` - shared flow widgets
- `feature/flow/presentation/FlowPresentationHelpers.kt` - helper logic

## Asset contracts

- `app/src/main/assets/templates/` contains template manifest assets.
- `app/src/main/assets/layouts/` contains layout definition JSON and image assets.
- `docs/json-schemas/` documents the app-defined JSON formats.

## Current structural goal

The project is currently aimed at preserving the working flow while reducing complexity in the feature layer. The main goal is to keep the `flow` feature easier to understand and safer to refactor without breaking capture, assignment, or print behavior.
