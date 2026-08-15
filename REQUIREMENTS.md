# KawaiiPB Requirements

Last updated: 2026-08-15

## Functional requirements

### Session flow

- The app should support a kiosk-style guided photo booth session.
- The user should be able to choose a camera mode.
- Captured frames should be saved for assignment and print composition.
- The flow should support photo assignment into strip slots.
- The user should be able to select a strip layout and template overlay.
- Sticker and transform editing should remain available in the assignment flow.
- The app should support preview and print output generation.
- Admin mode should still expose timer and camera configuration options.

### Admin flow

- Admin entry should remain available from the landing screen flow.
- Configurable timer values should be persisted through storage settings.
- Camera selections should persist per mode.

### Storage behavior

- Captured content and exports should be saved into app-managed folders and public export destinations when available.
- Session logs should be stored locally for debugging.
- Asset loading must remain resilient when files are missing or unavailable.

## Technical requirements

- Android app built with Kotlin.
- Jetpack Compose used for the UI layer.
- CameraX used for capture flow behavior.
- Local file access used for output and session resources.
- JSON-based layout and template metadata should remain aligned with asset files.

## Environment requirements

- Android Studio or equivalent IDE
- Android SDK configured for the project
- Java version compatible with the Gradle setup
- Device or emulator with camera support for capture validation

## Current build requirement

The project is expected to compile successfully with the configured Gradle wrapper and Android tooling. Current verification in this workspace:

- `./gradlew.bat :app:compileDebugKotlin` -> successful

## Quality requirements

- Refactors should preserve current flow behavior unless explicitly changed.
- Shared UI should remain reusable across screens.
- Storage and print operations should remain isolated behind service boundaries.
- Flow changes should be validated on a device or emulator because behavior is UI-sensitive.

## Recommended validation checklist

Before finalizing a refactor or feature change, validate:

- capture flow still records frames correctly
- assignment transforms still update the correct slot
- template/layout path resolution still works
- print output still renders successfully
- admin settings still persist and reload as expected
