# KawaiiPB

KawaiiPB is an Android kiosk-style app built with Kotlin and Jetpack Compose. It includes a landing screen, camera/photo workflow, drawing/sticker assignment, and an admin dashboard.

## Key Features

- Jetpack Compose UI
- Camera capture using CameraX
- Multi-step kiosk flow with session handling
- Photo assignment and layout selection
- Drawing tools, stickers, and template support
- Admin dashboard with PIN validation
- Local storage and session logging

## Project Structure

- `app/` - Android application module
- `app/src/main/java/com/zcamstudio/kawaiipb/` - app source packages
- `app/src/main/res/` - Android resources
- `build.gradle.kts` - top-level Gradle configuration
- `settings.gradle.kts` - project module settings
- `gradle/` - Gradle wrapper and version catalog configuration

## Requirements

- Android SDK 36
- Java 11
- Gradle wrapper included in the repository
- Android device/emulator with camera support for camera flow

## Build and Run

From the project root on Windows:

```powershell
cd "d:\My Program\KawaiiPB"
.\gradlew.bat :app:assembleDebug
```

To install the debug build on a connected device/emulator:

```powershell
.\gradlew.bat :app:installDebug
```

## Tests

Run unit tests for the app module:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Notes

- The app uses an in-memory kiosk repository for local flow data.
- Camera permission is declared in `app/src/main/AndroidManifest.xml`.
- The UI is styled with a custom Material3 theme.

## Getting Started

1. Open the project in Android Studio.
2. Let Gradle sync and ensure the SDK/NDK settings are correct.
3. Build and run the `app` module on a device or emulator.

## Contact

This repository does not include a license file. Update the README with project-specific maintainers or license details if needed.
