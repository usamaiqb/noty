# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.noty.utils.ThemeManagerTest"

# Run instrumentation tests
./gradlew connectedAndroidTest

# Install debug APK on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean build
```

## Architecture

This is an Android note-taking app with persistent notifications using **MVVM + Repository pattern**.

### Layer Structure

```
com.example.noty/
├── data/           # Data layer (Room database, entities, repository)
├── ui/             # Presentation layer (Activities, ViewModel, Adapters)
└── utils/          # Utilities (notifications, theme, services, receivers)
```

### Data Flow

1. **Room Database** (`AppDatabase`, `TaskDao`, `Task`) → stores tasks persistently
2. **Repository** (`NotyRepository`) → abstracts database operations, exposes Flow
3. **ViewModel** (`NotyViewModel`) → manages UI state, controls foreground service lifecycle
4. **UI** (`MainActivity`, `TaskAdapter`) → observes LiveData, renders tasks

### Key Architectural Decisions

- **Reactive data**: Room queries return `Flow<List<Task>>`, ViewModel exposes `LiveData`
- **Foreground service** (`StickyService`): Auto-starts when tasks exist, auto-stops when empty
- **Notification sync**: Each task maps to a persistent notification via `NotificationHelper`
- **Theme persistence**: Uses `DataStore` with `ThemeManager` (System/Light/Dark modes)
- **Boot receiver**: `BootReceiver` restarts the service on device boot

### Important Patterns

- `NotyViewModelFactory` provides dependency injection for ViewModel
- `TaskAdapter` uses `ListAdapter` with `DiffUtil` for efficient RecyclerView updates
- `NotificationReceiver` handles notification action broadcasts (delete/dismiss)
- Database uses `fallbackToDestructiveMigration()` - data loss on schema changes

## Configuration

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Kotlin**: 1.9.20
- **Room**: 2.6.1 (with KSP)
- **View Binding & Data Binding**: Enabled

## Permissions

The app requires:
- `POST_NOTIFICATIONS` (runtime permission, API 33+)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC`
- `RECEIVE_BOOT_COMPLETED`
