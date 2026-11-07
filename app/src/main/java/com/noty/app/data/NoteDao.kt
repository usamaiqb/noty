package com.noty.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.noty.app.data.backup.ImportPlan
import kotlinx.coroutines.flow.Flow

// One function per query is how Room DAOs are written; splitting this one would only
// scatter the notes table's queries.
@Suppress("TooManyFunctions")
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

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesOnce(): List<Note>

    @Insert
    suspend fun insertNotes(notes: List<Note>): List<Long>

    @Update
    suspend fun updateNotes(notes: List<Note>)

    /**
     * Merges a backup in one transaction: the plan is made from what is stored at that
     * moment, and either every change lands or none does.
     */
    @Transaction
    suspend fun importNotes(incoming: List<Note>, now: Long, keepReminders: Boolean): ImportResult {
        val plan = ImportPlan.of(incoming, getAllNotesOnce(), now, keepReminders)
        val ids = insertNotes(plan.toInsert)
        updateNotes(plan.toReplace)
        val inserted = plan.toInsert.zip(ids) { note, id -> note.copy(id = id) }
        return ImportResult(changedNotes = inserted + plan.toReplace, unchanged = plan.unchanged)
    }
}

/**
 * @param changedNotes the notes that were added or replaced, with their row ids, so their
 *   notifications and alarms can be brought in line.
 * @param unchanged notes in the file that were already here, as recent or more recent.
 */
data class ImportResult(val changedNotes: List<Note>, val unchanged: Int)
