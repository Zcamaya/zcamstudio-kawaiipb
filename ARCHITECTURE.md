# KawaiiPB Project Structure and Architecture

Last updated: 2026-08-12

## Overview

KawaiiPB is an Android application built with Kotlin and Jetpack Compose. The project uses a feature-oriented, layered structure that keeps the UI, state, domain logic, and data access separated enough to be maintainable while still being practical for kiosk-style flow work.

## Architectural Style

The app follows a hybrid approach:

- Feature-first organization
- Compose-based UI layer
- ViewModel-driven state management
- Repository abstraction for data access
- Domain models and helper functions for business logic

This is not a strict Clean Architecture setup, but it is organized to support incremental refactoring.

## Main Project Structure

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
      assets/
      res/
```

## Key Folders

### app/
Application entry points and top-level app wiring.

- `KawaiiPbApp.kt`: app-level initialization
- `MainActivity.kt`: activity hosting the Compose app

### core/
Shared infrastructure and reusable foundation pieces.

- `core/designsystem/`: reusable Compose UI components, theme, colors, buttons, cards
- `core/viewmodel/`: shared ViewModel utilities or base patterns

### domain/
Business/domain layer.

- `domain/model/`: core models such as flow state, templates, layouts, stickers
- `domain/repository/`: repository interfaces
- `domain/usecase/`: use cases or business logic units

### data/
Data implementation layer.

- `data/repository/`: concrete repository implementations
- the current flow uses local/in-memory data sources for the prototype and kiosk session state

### feature/
Feature-based UI modules.

- `feature/landing/`: landing screen experience
- `feature/flow/`: kiosk flow experience, stage UI, ViewModel, and helpers
- `feature/admin/`: admin experience
- `feature/printing/`: print composition and output helpers

### navigation/
Navigation graph and app routing.

- `navigation/KawaiiNavHost.kt`

### services/
Cross-cutting services.

- `services/storage/`: storage service implementations
- `services/logging/`: session logging services

## How the App Is Organized

### Presentation layer
The UI is built with Jetpack Compose and lives in feature-specific presentation files.

Examples:
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowScreen.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowStageSections.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowAssignmentSections.kt`

### State layer
ViewModels manage UI state and flow events.

Example:
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowViewModel.kt`

### Domain layer
Domain models and interfaces define the app concepts.

Example:
- `app/src/main/java/com/zcamstudio/kawaiipb/domain/model/`

### Data layer
Repositories provide concrete data access implementations.

Example:
- `app/src/main/java/com/zcamstudio/kawaiipb/data/repository/`

## Typical Flow

1. User interacts with a Compose screen.
2. The screen sends callbacks or ViewModel actions.
3. The ViewModel updates state.
4. Compose re-renders from state.
5. Services and repositories provide file, session, and asset data as needed.

## Current Flow Notes

The active kiosk flow is centered around:

- landing and session entry
- camera capture
- photo assignment
- template selection and backdrop control
- preview and print preparation
- admin access

The template tab currently combines template selection with an embedded custom color experience, so layout/docs should be kept in sync with that UI behavior.

## Strengths of the Current Structure

- Clear feature separation
- Reusable design system components
- Easier maintenance than a single-screen app
- Good fit for incremental refactoring

## Current Refactoring Focus

The current cleanup work is focused on:

- reducing screen complexity
- extracting repeated UI logic into smaller composables
- keeping flow state and UI easier to navigate
- preserving app behavior and layout

## Most Important Files

- `app/src/main/java/com/zcamstudio/kawaiipb/MainActivity.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/KawaiiPbApp.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/navigation/KawaiiNavHost.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowScreen.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/feature/flow/presentation/FlowViewModel.kt`
- `app/src/main/java/com/zcamstudio/kawaiipb/core/designsystem/`
- `app/src/main/java/com/zcamstudio/kawaiipb/domain/model/`
- `app/src/main/java/com/zcamstudio/kawaiipb/data/repository/`
