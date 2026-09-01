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
#
# Usage:
#   .\scripts\seed_notes.ps1                    # seed notes into debug build
#   .\scripts\seed_notes.ps1 -Clear            # wipe existing notes first
#   .\scripts\seed_notes.ps1 -Package com.noty.app.debug  # explicit package
# ─────────────────────────────────────────────────────────────────────────────

param(
    [string]$Package = "com.noty.app",
    [switch]$Clear
)

$DB_NAME  = "noty_database"
$DB_PATH  = "/data/data/$Package/databases/$DB_NAME"

# ── helper: run a SQL statement via adb ──────────────────────────────────────
function Invoke-Sql([string]$sql) {
    adb shell "run-as $Package sqlite3 $DB_PATH `"$sql`""
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
#  Schema: id (auto), title, description, type (NOTE|REMINDER|WORK), timestamp, isPinned (1/0)
$notes = @(
    # ── Pinned notes ──────────────────────────────────────────────────────────
    @{
        title       = "Review pull request"
        description = "#42 — auth refactor"
        type        = "WORK"
        timestamp   = $now - $oneHour
        isPinned    = 1
    },
    @{
        title       = "Call dentist"
        description = "Book appointment for Tuesday"
        type        = "REMINDER"
        timestamp   = $now - (2 * $oneHour)
        isPinned    = 1
    },
    @{
        title       = "Buy groceries"
        description = "Milk, eggs, bread, coffee"
        type        = "NOTE"
        timestamp   = $now - (3 * $oneHour)
        isPinned    = 1
    },

    # ── Unpinned notes ────────────────────────────────────────────────────────
    @{
        title       = "Weekend reading"
        description = "Finish chapter 4 of Atomic Habits and start Deep Work."
        type        = "NOTE"
        timestamp   = $now - $oneDay
        isPinned    = 0
    },
    @{
        title       = "Team lunch"
        description = "Friday 1 PM at the Italian place on 5th. Confirm with Sara and Jake."
        type        = "REMINDER"
        timestamp   = $now - (2 * $oneDay)
        isPinned    = 0
    },
    @{
        title       = "Team lunch"
        description = "Friday 1 PM at the Italian place on 5th. Confirm with Sara and Jake."
        type        = "REMINDER"
        timestamp   = $now - (2 * $oneDay)
        isPinned    = 0
    }
)

# ── insert notes ─────────────────────────────────────────────────────────────
Write-Host "`nInserting $($notes.Count) sample notes..." -ForegroundColor Cyan

foreach ($note in $notes) {
    # Escape single-quotes in text for SQLite
    $title = $note.title -replace "'", "''"
    $desc  = $note.description -replace "'", "''"

    $sql = "INSERT INTO notes (title, description, type, timestamp, isPinned) " +
           "VALUES ('$title', '$desc', '$($note.type)', $($note.timestamp), $($note.isPinned));"

    Invoke-Sql $sql
    $pin = if ($note.isPinned) { "[pinned]" } else { "        " }
    Write-Host "  + $pin $($note.title)" -ForegroundColor Green
}

# ── verify ────────────────────────────────────────────────────────────────────
Write-Host ""
$count = Invoke-Sql "SELECT COUNT(*) FROM notes;"
Write-Host "Total notes in database: $count" -ForegroundColor Cyan

Write-Host "`nDone! Restart the app or pull-to-refresh to see the notes." -ForegroundColor Magenta
