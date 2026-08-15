# Phase Status

Last updated: 2026-08-15

## Verified status

The project was verified with:

- `./gradlew.bat :app:compileDebugKotlin` -> successful

This is the current project health check available in this workspace. It confirms that the app compiles in its current state.

## Current focus areas

### Flow feature cleanup

The main active area is the kiosk flow in `feature/flow/presentation`.

Current concerns:

- large state model in `FlowUiState.kt`
- dense transition logic in `FlowViewModel.kt`
- multiple stage-specific actions concentrated in a few files
- higher regression risk around capture, assignment, and print behavior

### Print and asset pipelines

The app is also actively relying on:

- `feature/printing/` render helpers
- asset template discovery logic
- layout JSON resolution from `app/src/main/assets/layouts`

These are important to keep in sync with the UI flow and JSON schema docs.

### Admin and configuration

Admin flow and timer configuration are present and working as feature-level entries, but they should be reviewed when the flow logic is refactored to ensure the same configuration values remain intact.

## Recommended next steps

1. Reduce the complexity of the flow state model.
2. Split stage-specific logic into smaller, isolated composables.
3. Add targeted regression checks around capture and photo assignment.
4. Keep the JSON schema documentation aligned with actual asset files.
5. Only after that, consider broader architectural cleanup.

## Current status summary

- Build status: verified compile success
- Main refactor target: flow feature and state model
- Highest risk: behavior regressions in capture, layout, and print flow
- Lowest-risk improvement: document cleanup and contract alignment
