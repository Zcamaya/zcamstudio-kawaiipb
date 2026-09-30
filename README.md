# KawaiiPB

Last updated: 2026-08-15

KawaiiPB is an Android kiosk-style photo booth app built with Kotlin and Jetpack Compose. The app currently centers on a guided flow for session startup, camera capture, photo assignment, strip layout selection, preview, printing, and admin configuration.

## Current project status

The project is currently in an active feature-refactor state. The most recent verification performed in this workspace was:

- `./gradlew.bat :app:compileDebugKotlin` -> successful

This confirms the current app builds in its present state, although the flow feature remains the highest-risk area for behavior changes during refactoring.

## Current flow

1. Landing screen starts a new kiosk session.
2. User chooses a camera mode.
3. Capture stage records photo frames.
4. Photo assignment stage maps captured photos to layout slots.
5. Strip layout and template selection are applied to the selected output.
6. Preview and print stages render the final sheet and save output.
7. Admin route exposes timer and camera configuration actions.

## Key features

- Jetpack Compose UI
- CameraX capture flow
- Multi-stage kiosk session flow
- Photo assignment and strip layout selection
- Template overlay handling from asset and external directories
- Sticker and transform editing in the assignment flow
- Local storage and session logging
- Admin dashboard for timer and camera settings

## Project structure

- `app/` - Android application module
- `app/src/main/java/com/zcamstudio/kawaiipb/` - app source packages
- `app/src/main/res/` - Android resources
- `app/src/main/assets/` - template and layout assets
- `docs/json-schemas/` - JSON format documentation
- `build.gradle.kts` - top-level Gradle configuration
- `settings.gradle.kts` - project module settings

## Architecture snapshot

This project follows a feature-first organization, not a strict Clean Architecture layout. The app is organized around:

- `feature/*` for screen-specific logic
- `domain/*` for model and use-case contracts
- `data/*` for in-memory data access
- `services/*` for storage and logging
- `core/designsystem/*` for shared Compose UI components

The flow feature is the main source of complexity and the most likely refactor target.

## Build and run

From the project root on Windows:

```powershell
cd "d:\My Program\KawaiiPB"
.\gradlew.bat :app:assembleDebug
```

If you want to just validate compilation:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

### Native Windows target

The repository now includes a Compose Desktop Windows target and a shared Kotlin module.
The desktop target is the foundation for the cross-platform port; the existing Android flow
continues to build unchanged while camera, storage, and printing adapters are migrated.

Compile the Windows target with:

```powershell
.\gradlew.bat :desktopApp:compileKotlin
```

Run the native desktop window with:

```powershell
.\gradlew.bat :desktopApp:run
```

Create Windows installers with:

```powershell
.\gradlew.bat :desktopApp:packageMsi
```

Windows packaging requires a full JDK that includes `jpackage.exe`; Android Studio's bundled
runtime may not include it. The current desktop launcher is intentionally a migration foundation.
Camera capture, Windows file storage, image/PDF rendering, printer integration, and the full
session flow still need desktop implementations.

## Important docs

- [Architecture overview](ARCHITECTURE.md)
- [System structure](SYSTEM_STRUCTURE.md)
- [Requirements](REQUIREMENTS.md)
- [Roadmap](ROADMAP.md)
- [JSON schema docs](docs/json-schemas/README.md)

## Notes

- App permissions are handled in `KawaiiPbApp.kt` for storage/media access.
- The navigation graph is defined in `navigation/KawaiiNavHost.kt`.
- The flow stage and screen logic are concentrated in the `feature/flow/presentation` package.
- Template and layout JSON contracts are documented in the docs folder and should stay aligned with the actual asset files.

## License

No license file is currently included in this repository.
