## [1.0.8] - 2026-11-27

### Added
- Schedule a note to pin later. A note is now unpinned, pinned, or scheduled to pin at a set time, never both pinned and scheduled. At that time it is pinned and posted once with an alert, then stays pinned like any other note. In the editor, Pin is a one-tap toggle and a clock sets a time. Once a time is set, a menu offers Don't pin, Pin now, or Pin later. Scheduled notes show a clock and their time on the list, as "Today, 4:24 PM", "Tomorrow, 9:00 AM" or a date. The alert channel is renamed "Scheduled notes" and keeps your existing settings (#104)
- A full-screen note editor replaces the bottom sheet. It has room for long text, and the description fills the screen and scrolls. Close, back and the back gesture ask before discarding unsaved changes. An existing note can be deleted from the editor. Save, pin and schedule sit in a floating toolbar above the keyboard (#96)
- Czech translation, contributed by TomawsCZ (#105)
- Complete Spanish translation (#94)

### Improved
- Complete German translation (#90)
- Long-pressing a note toggles its pin again, and TalkBack announces it as "Pin note" or "Unpin note" (#96)
- Settings switches are announced by screen readers as switches with their on/off state (#89)

### Fixed
- With dynamic color on a light theme, the check on a turned-on switch was invisible (#89)
- Tapping ✓ on an unchanged note no longer rewrites it or re-posts its notification (#96)

### Changed
- Updated the Android Gradle Plugin to 9.5.1, Gradle to 9.9.1, KSP to 2.3.13 and kotlinx-serialization-json (#85, #86, #92, #93)
- The release workflow passes the secrets the Google Play upload needs (#88)

## [1.0.7] - 2026-11-07

### Added
- Back up notes to a file and restore them: export writes a JSON file through the system file picker, and import merges it in. A note already on the device is replaced only by a more recently edited copy, so importing the same file twice changes nothing. Reminders already in the past, or all reminders if exact alarms aren't allowed, are dropped on import (#24, #75)
- Notes now record when they were last edited. The list is sorted by that time and each card shows it (#74)
- When notifications are turned off, Home and Settings show a card explaining it, with a way to turn them back on. Pin controls appear disabled until they are, instead of failing silently (#72)
- A failed save, update or delete now shows a message instead of quietly losing the change. So does a reminder dropped because its permission was revoked or its time passed (#72)

### Improved
- Expressive spring motion across components through MaterialExpressiveTheme (#68)
- More complete Simplified Chinese translation (#78)

### Fixed
- Swiping away a notification could delete its note. Dismissing a notification now only brings a pinned note's notification back (#72)
- The add-note sheet could reopen after a recomposition because a callback went stale (#67)

### Changed
- Database schema v8: notes get a stable ID for backups and a last-edited timestamp, with tested migrations from v6 (#74, #75)
- Dropped LiveData. The app now collects Flows with lifecycle awareness and has three fewer dependencies (#66)
- Google Play builds use the `noty.app` application ID, and tagged releases upload to Google Play along with the store listing. F-Droid and GitHub builds keep `com.noty.app` (#64, #76)
- Room schemas are exported, and CI tests the migrations on an emulator (#70)
- Updated the Android Gradle Plugin to 9.5.0 (#63)

## [1.0.6] - 2026-10-29

### Added
- German and Simplified Chinese translations (#50, #51)
- Per-app language selection on Android 13+: Noty now appears in the system's per-app language settings, and Settings has a Language row that opens that page (#60)

### Improved
- A complete green default color palette, built from Material Green 500, for devices without dynamic color or with it turned off; previously most color roles fell back to Material's baseline purple (#59)
- Changing between Light and Dark theme now recomposes in place instead of recreating the screen
- Settings and the notes list now use the shared expressive components: consistent switches, rows, spacing and haptics
- The About screen shows just the version name in release builds, without the build number

### Fixed
- Removed an Android 12+ splash screen override that referenced theme colors the splash theme can't resolve (#58)

### Changed
- Dropped the androidx.appcompat dependency
- Added a debug build variant that installs alongside the release app (#55)
- Updated the Android Gradle Plugin to 9.4.1, Gradle to 9.8.2, and the Compose compiler plugin and detekt Compose rules

## [1.0.5] - 2026-10-11

### Fixed
- Expanded reminder notifications now show the note's full description instead of a truncated line (#38)
- The keep-alive service now runs as a `specialUse` foreground service instead of `dataSync`, so it isn't stopped by Android's limits on that service type (#39)

### Changed
- Target Android 16 (API 36)
- Moved all user-facing text into string resources, ready for translation
- Updated Material Components to 1.14.0, KSP to 2.3.12, and the Kotlin Compose compiler plugin
- Build and code-quality cleanup: a version catalog for dependencies, detekt with Compose rules in CI, lint policy in `lint.xml`, and removal of unused resources and API checks made redundant by minSdk 26

## [1.0.4] - 2026-09-25

### Added
- One-shot date/time reminders for notes, scheduled from the note editor, with exact-alarm delivery and permission handling
- A Settings screen with theme selection (Light/Dark/System), dynamic color, and pin defaults

## [1.0.3] - 2026-09-01

### Added
- Pinned notes now surface in a dedicated "Pinned" section above the rest (#14)

### Improved
- Restyled the notes list with a connected-pill design, rounded icon chips, and relative timestamps
- Added a collapsing top bar with in-app-bar search, list add/remove/reorder animations, and haptic feedback when pinning
- Upgraded Compose BOM to 2025.12.00

## [1.0.2] - 2026-08-01

### Added
- Expressive haptic feedback for user interactions (confirm, toggle, reject) using Compose UI haptics

### Improved
- Redesigned top bar with a collapsing header, auto-focus search mode, and responsive FAB scroll behavior
- Added smooth list animations for note addition, removal, and reordering
- Updated Compose BOM to 2025.12.00

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
