package com.noty.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val type:  NoteType,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = true,
    val reminderAt: Long? = null
)

enum class NoteType {
    NOTE, REMINDER, WORK
}
