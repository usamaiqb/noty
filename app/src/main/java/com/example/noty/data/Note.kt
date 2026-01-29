package com.example.noty.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val type:  NoteType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class NoteType {
    NOTE, REMINDER, WORK
}
