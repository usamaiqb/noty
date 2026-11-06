package com.noty.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
