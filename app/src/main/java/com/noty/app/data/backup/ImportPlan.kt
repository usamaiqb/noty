package com.noty.app.data.backup

import com.noty.app.data.Note

/**
 * What importing a backup will do, worked out before anything is written.
 *
 * The rule: a note is the same note when its uuid matches. A note that isn't here yet is
 * added. A note that is here is replaced only when the file's copy was edited more recently
 * (a later [Note.updatedAt]); otherwise this device's copy stays. Importing the same file
 * twice changes nothing.
 */
data class ImportPlan(
    val toInsert: List<Note>,
    /** The file's copies, carrying this device's row id. */
    val toReplace: List<Note>,
    val unchanged: Int
) {
    companion object {
        /**
         * @param keepReminders false when reminders can't be scheduled right now (no exact
         *   alarm permission). Imported reminders are then dropped, as they are when saving
         *   a note, rather than stored with no alarm behind them. Reminders already in the
         *   past are always dropped.
         */
        fun of(incoming: List<Note>, existing: List<Note>, now: Long, keepReminders: Boolean): ImportPlan {
            val byUuid = existing.associateBy { it.uuid }
            // A hand-edited file could list a note twice; the latest edit wins there too.
            val latest = incoming
                .groupBy { it.uuid }
                .map { (_, copies) -> copies.maxBy { it.updatedAt } }

            val toInsert = mutableListOf<Note>()
            val toReplace = mutableListOf<Note>()
            var unchanged = incoming.size - latest.size
            for (note in latest) {
                val reminder = note.reminderAt?.takeIf { keepReminders && it > now }
                val clean = note.copy(id = 0, reminderAt = reminder)
                val local = byUuid[note.uuid]
                when {
                    local == null -> toInsert += clean
                    note.updatedAt > local.updatedAt -> toReplace += clean.copy(id = local.id)
                    else -> unchanged++
                }
            }
            return ImportPlan(toInsert, toReplace, unchanged)
        }
    }
}
