package com.noty.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC, id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert
    suspend fun insertNote(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    /** For the user's edits from the note sheet. Writes every column. */
    @Update
    suspend fun updateNote(note: Note)

    // Changes the app makes on its own (a notification action, a reminder firing) touch only
    // their column, so they can't overwrite an edit made in the meantime with a stale copy.

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :noteId")
    suspend fun setPinned(noteId: Long, pinned: Boolean)

    @Query("UPDATE notes SET reminderAt = NULL WHERE id = :noteId")
    suspend fun clearReminder(noteId: Long)

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE reminderAt IS NOT NULL")
    suspend fun getReminderNotes(): List<Note>
}
