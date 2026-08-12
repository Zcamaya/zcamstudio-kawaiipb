# KawaiiPB System Structure

Last updated: 2026-08-12

## Purpose

KawaiiPB is an Android kiosk application for a guided photo booth workflow. The app combines camera capture, photo assignment, template selection, preview, printing, and admin access in one Compose-based experience.

## Top-Level Layout

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
        ui/
      res/
      assets/
    test/
docs/
  json-schemas/
```

## Main Subsystems

### App Bootstrap

- `MainActivity.kt`: launches the Compose app
- `KawaiiPbApp.kt`: app root and dependency wiring

### Navigation

- `navigation/KawaiiNavHost.kt`: route selection and screen switching

### Feature Layer

- `feature/landing/`: landing and session entry
- `feature/flow/`: kiosk flow, stage UI, flow ViewModel, and helpers
- `feature/printing/`: print composition and print pipeline helpers
- `feature/admin/`: admin dashboard and PIN workflow

### Domain Layer

- `domain/model/`: core business entities and UI state models
- `domain/repository/`: repository contracts
- `domain/usecase/`: business actions and data access orchestration

### Data Layer

- `data/repository/`: concrete repository implementations
- currently the app uses lightweight local/in-memory data sources

### Services

- `services/storage/`: file and session storage helpers
- `services/logging/`: session logging and event tracking

### Core UI

- `core/designsystem/`: reusable Compose theme, colors, buttons, cards, and layout components
- `core/viewmodel/`: shared ViewModel utilities if needed by multiple features

## Runtime Flow

1. The app starts in `MainActivity`.
2. `KawaiiPbApp` wires dependencies and theme.
3. `KawaiiNavHost` chooses the active screen.
4. Feature screens render state from their ViewModels.
5. User actions update state through callbacks or ViewModel methods.
6. Services and repositories provide storage, logging, and domain data.

## Current File Ownership

- `FlowScreen.kt`: coordinator for the kiosk flow UI
- `FlowViewModel.kt`: flow state and transitions
- `FlowPresentationHelpers.kt`: shared flow presentation utilities
- `FlowStageSections.kt`: stage-specific flow screens
- `FlowCaptureSections.kt`: camera capture UI
- `FlowAssignmentSections.kt`: photo assignment, template selection, and custom color UI
- `FlowSharedSections.kt`: reusable flow preview/editor widgets
- `FlowViewModelSupport.kt`: shared flow ViewModel helper functions

## JSON Format Docs

- `docs/json-schemas/template.schema.md`
- `docs/json-schemas/layout-manifest.md`

## Structural Goals

- Keep feature code inside feature packages
- Keep shared UI in `core/designsystem`
- Keep business rules in `domain`
- Keep storage and logging behind small service boundaries
- Avoid single files accumulating unrelated responsibilities
