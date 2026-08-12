# KawaiiPB

Last updated: 2026-08-12

[![Android CI](https://github.com/Zcamaya/zcamstudio-kawaii-pb/actions/workflows/android.yml/badge.svg)](https://github.com/Zcamaya/zcamstudio-kawaii-pb/actions/workflows/android.yml)

KawaiiPB is an Android kiosk-style photo booth app built with Kotlin and Jetpack Compose. The current flow covers landing, camera capture, photo assignment, template selection, preview/print preparation, and admin access.

## Current Flow

1. Landing screen opens the kiosk session.
2. User selects a camera mode and captures photos.
3. Photo assignment stage places captured photos into the strip layout.
4. Template tab lets the user choose a backdrop or template asset.
5. Preview and print stages reuse the same layout data for output.
6. Admin mode provides PIN-protected access to management actions.

## Key Features

- Jetpack Compose UI
- Camera capture using CameraX
- Guided multi-step kiosk flow
- Photo assignment and strip layout selection
- Template selection with asset-backed manifests
- Drawing/sticker support where enabled by the flow
- Admin dashboard with PIN validation
- Local storage and session logging

## Project Structure

- `app/` - Android application module
- `app/src/main/java/com/zcamstudio/kawaiipb/` - app source packages
- `app/src/main/res/` - Android resources
- `app/src/main/assets/` - template and layout JSON assets
- `docs/json-schemas/` - markdown source docs for the JSON formats used by the app
- `build.gradle.kts` - top-level Gradle configuration
- `settings.gradle.kts` - project module settings

## Requirements

- Android SDK 36
- Java 11
- Gradle wrapper included in the repository
- Android device or emulator with camera support for the capture flow

## Build, Run, and Debug

From the project root on Windows:

```powershell
cd "d:\My Program\KawaiiPB"
.\gradlew.bat :app:assembleDebug
```

To install the debug build on a connected device or emulator:

```powershell
.\gradlew.bat :app:installDebug
```

To run unit tests:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Useful debug checks when a change touches the flow UI or templates:

- `.\gradlew.bat :app:compileDebugKotlin`
- Review Logcat in Android Studio for flow crashes or asset-loading failures
- Confirm the template and layout JSON files still resolve from `app/src/main/assets/`

## Continuous Integration

This repository includes a GitHub Actions workflow at `.github/workflows/android.yml`.

- Builds the `app` module with `assembleDebug`
- Runs `:app:testDebugUnitTest`

## JSON Format Docs

- [Template manifest schema](docs/json-schemas/template.schema.md)
- [Layout manifest format](docs/json-schemas/layout-manifest.md)

## Notes

- The app uses local repository and storage services for kiosk flow data.
- Camera permission is declared in `app/src/main/AndroidManifest.xml`.
- The UI is styled with a custom Material3 theme.

## Getting Started

1. Open the project in Android Studio.
2. Let Gradle sync and confirm the Android SDK is installed.
3. Build and run the `app` module on a device or emulator.

## License

No license file is included in this repository. Add a `LICENSE` file to declare project licensing and maintainers.
