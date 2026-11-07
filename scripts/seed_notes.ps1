# ─────────────────────────────────────────────────────────────────────────────
# seed_notes.ps1
#
# Seeds sample notes into the Noty app database on a connected emulator/device
# for screenshot purposes.
#
# Requirements:
#   - adb in your PATH (Android SDK platform-tools)
#   - The app must already be installed (run it at least once so the DB exists)
#   - An emulator or physical device connected
#   - A release build needs a rootable emulator (a "Google APIs" image, not
#     "Google Play"): run-as only works on debuggable builds, so the script
#     falls back to `adb root` for those
#
# Usage:
#   .\scripts\seed_notes.ps1                    # seed notes into the release build
#   .\scripts\seed_notes.ps1 -Clear            # wipe existing notes first
#   .\scripts\seed_notes.ps1 -Package com.noty.app.debug  # the debug build
#
# Pinned notes only reach the notification shade once the app posts them:
# reinstall the APK afterwards (MY_PACKAGE_REPLACED restores them) or reboot.
# ─────────────────────────────────────────────────────────────────────────────

param(
    [string]$Package = "com.noty.app",
    [switch]$Clear
)

$DB_NAME  = "noty_database"
$DB_PATH  = "/data/data/$Package/databases/$DB_NAME"

# ── helper: run a SQL statement via adb ──────────────────────────────────────
$script:UseRoot = $false
function Invoke-Sql([string]$sql) {
    if ($script:UseRoot) {
        adb shell "sqlite3 $DB_PATH `"$sql`""
    } else {
        adb shell "run-as $Package sqlite3 $DB_PATH `"$sql`""
    }
}

# ── verify adb is available ──────────────────────────────────────────────────
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error "adb not found. Add Android SDK platform-tools to your PATH."
    exit 1
}

# ── verify device is connected ───────────────────────────────────────────────
$devices = adb devices | Select-String "device$"
if (-not $devices) {
    Write-Error "No device/emulator connected. Start an emulator or plug in a device."
    exit 1
}

Write-Host "Connected device(s):" -ForegroundColor Cyan
adb devices

# ── pick run-as (debuggable build) or root (release build) ───────────────────
adb shell "run-as $Package true" 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "$Package isn't debuggable; using adb root instead of run-as." -ForegroundColor Yellow
    adb root | Out-Null
    adb wait-for-device
    $script:UseRoot = $true
}

# Room keeps the database open; stop the app so it can't overwrite the changes.
adb shell "am force-stop $Package"

# ── optionally clear existing notes ─────────────────────────────────────────
if ($Clear) {
    Write-Host "`nClearing existing notes..." -ForegroundColor Yellow
    Invoke-Sql "DELETE FROM notes;"
    Write-Host "Done." -ForegroundColor Green
}

# ── timestamp helpers (milliseconds since epoch) ─────────────────────────────
$now      = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$oneHour  = 3600000
$oneDay   = 86400000
$oneWeek  = 7 * $oneDay

# ── sample notes ─────────────────────────────────────────────────────────────
#  Schema (v8): id (auto), uuid, title, description, type (NOTE|REMINDER|WORK),
#  createdAt, updatedAt, isPinned (1/0), reminderAt
$notes = @(
    # ── Pinned notes ──────────────────────────────────────────────────────────
    @{
        title       = "Review pull request"
        description = "#42 — auth refactor"
        type        = "WORK"
        createdAt   = $now - $oneHour
        isPinned    = 1
    },
    @{
        title       = "Call dentist"
        description = "Book appointment for Tuesday"
        type        = "REMINDER"
        createdAt   = $now - (2 * $oneHour)
        isPinned    = 1
    },
    @{
        title       = "Buy groceries"
        description = "Milk, eggs, bread, coffee"
        type        = "NOTE"
        createdAt   = $now - (3 * $oneHour)
        isPinned    = 1
    },

    # ── Unpinned notes ────────────────────────────────────────────────────────
    @{
        title       = "Weekend reading"
        description = "Finish chapter 4 of Atomic Habits and start Deep Work."
        type        = "NOTE"
        createdAt   = $now - $oneDay
        isPinned    = 0
    },
    @{
        title       = "Team lunch"
        description = "Friday 1 PM at the Italian place on 5th. Confirm with Sara and Jake."
        type        = "REMINDER"
        createdAt   = $now - (2 * $oneDay)
        isPinned    = 0
    },
    @{
        title       = "Gift ideas for Mom"
        description = "Silk scarf, the cookbook she mentioned, or a pottery class"
        type        = "NOTE"
        createdAt   = $now - (4 * $oneDay)
        isPinned    = 0
    }
)

# ── insert notes ─────────────────────────────────────────────────────────────
Write-Host "`nInserting $($notes.Count) sample notes..." -ForegroundColor Cyan

foreach ($note in $notes) {
    # Escape single-quotes in text for SQLite
    $title = $note.title -replace "'", "''"
    $desc  = $note.description -replace "'", "''"

    $uuid  = [guid]::NewGuid().ToString()

    $sql = "INSERT INTO notes (uuid, title, description, type, createdAt, updatedAt, isPinned) " +
           "VALUES ('$uuid', '$title', '$desc', '$($note.type)', $($note.createdAt), $($note.createdAt), $($note.isPinned));"

    Invoke-Sql $sql
    $pin = if ($note.isPinned) { "[pinned]" } else { "        " }
    Write-Host "  + $pin $($note.title)" -ForegroundColor Green
}

# ── verify ────────────────────────────────────────────────────────────────────
Write-Host ""
$count = Invoke-Sql "SELECT COUNT(*) FROM notes;"
Write-Host "Total notes in database: $count" -ForegroundColor Cyan

# Files sqlite3 created as root (journal, -wal, -shm) would lock the app out of its own DB.
if ($script:UseRoot) {
    $owner = (adb shell "stat -c %u:%g /data/data/$Package").Trim()
    adb shell "chown $owner $DB_PATH*; restorecon $DB_PATH*"
}

Write-Host "`nDone! Start the app to see the notes." -ForegroundColor Magenta
