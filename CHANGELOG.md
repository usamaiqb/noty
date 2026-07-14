## [1.0.1] - 2026-07-14

### Improved
- Restyled the notes list with an expressive connected-pill design

### Fixed
- Resolved Room 2.7 API deprecation warnings (explicit exportSchema, updated fallbackToDestructiveMigration call)

## [1.0.0] - 2026-06-21

### Added
- Persistent notifications — every note appears as a pinned notification in the status bar
- Add, edit, and delete notes with a title and optional description
- Pin or unpin any note at any time, directly from the notification or from the app
- Notification actions — delete or unpin a note without opening the app
- Notes restored automatically after a device restart via boot receiver
- Quick Settings tile for adding a note without unlocking the phone
- Search bar to filter notes by title or description
- Material You design with dynamic color on Android 12+
- Light, Dark, and System theme options, persisted across sessions
- Foreground service to keep pinned notifications active when the app is closed
- Room database with schema migration support
- GPL v3 license
- F-Droid reproducible build setup
