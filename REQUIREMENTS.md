# KawaiiPB Requirements

Last updated: 2026-08-12

## Functional Requirements

### App Flow

- The app must support a kiosk-style guided session
- The user must be able to choose a camera mode
- The app must support live camera capture
- The app must support photo assignment after capture
- The app must support strip size selection
- The app must support template selection
- The app must support drawing and sticker placement
- The app must support preview before printing
- The app must support printing/exporting the final result
- The app must support QR-based session completion/download flow

### Admin Flow

- The app must support admin entry
- The app must validate the admin PIN
- The app must allow access to session or app management actions from admin mode

### Storage and Session Behavior

- Captured photos must be saved locally
- Print output must be saved locally
- Session logging must persist important events
- The app must keep flow state per session

## Technical Requirements

- Android app built with Kotlin
- UI must use Jetpack Compose
- Camera capture must use CameraX
- Local file IO must be supported for capture and print assets
- JSON layout/template data must be loadable from assets
- The app must compile and run with the configured Android SDK

## Environment Requirements

- Android Studio or compatible IDE
- Android SDK installed
- Java runtime compatible with the project Gradle setup
- Device or emulator with camera support for capture flow

## Build Requirements

- Debug build must succeed
- Unit tests must pass
- Gradle wrapper must be usable from the repository

## Quality Requirements

- Changes must not break the current kiosk flow
- UI updates must preserve the current visual identity unless explicitly changed
- Refactoring must be behavior-preserving
- Shared helpers should be reused instead of duplicated

## Performance Requirements

- Camera preview should remain responsive
- Bitmap loading should avoid unnecessary repeated decoding
- Print composition should avoid redundant image preparation
- Compose UI should minimize avoidable recomposition and layout churn

## Security and Safety Requirements

- Camera permission must be handled explicitly
- File access must stay local and predictable
- Bitmap and asset loading must fail safely
- Session data should not expose secrets

## Testing Requirements

- Unit tests should cover helper logic where practical
- Refactors should be verified with `:app:testDebugUnitTest`
- Changes affecting IO or rendering should be checked for regressions

