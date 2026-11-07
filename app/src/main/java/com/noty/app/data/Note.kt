package com.noty.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes", indices = [Index(value = ["uuid"], unique = true)])
data class Note(
    // A row number on this device only. Other devices reuse the same numbers for other notes.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // The note's identity everywhere else: it travels in backup files, so importing a backup
    // can recognise a note that is already here, on this device or any other.
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val type:  NoteType,
    val createdAt: Long = System.currentTimeMillis(),
    // When the title or description last changed. Pinning and reminders don't count: they
    // aren't edits to what the note says. The default only exists so the 6 -> 7 migration can
    // add the column; the migration then copies createdAt into it.
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = createdAt,
    val isPinned: Boolean = true,
    val reminderAt: Long? = null
)

/**
 * The note with new text. [Note.updatedAt] moves only when the text actually changed, so
 * opening a note and saving it untouched doesn't reorder the list.
 */
fun Note.withText(title: String, description: String?, now: Long = System.currentTimeMillis()): Note =
    if (title == this.title && description == this.description) {
        this
    } else {
        copy(title = title, description = description, updatedAt = now)
    }

enum class NoteType {
    NOTE, REMINDER, WORK
}
