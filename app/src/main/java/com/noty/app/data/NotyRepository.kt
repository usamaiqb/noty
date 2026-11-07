package com.noty.app.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

// Mirrors NoteDao, one function per operation.
@Suppress("TooManyFunctions")
class NotyRepository(private val noteDao: NoteDao) {

    companion object {
        private const val TAG = "NotyRepository"
    }

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    /** @return the new row id, or -1 if the insert failed. */
    suspend fun insert(note: Note): Long = attempt("insert note", -1L) { noteDao.insertNote(note) }

    suspend fun update(note: Note): Boolean = attempt("update note") { noteDao.updateNote(note) }

    suspend fun setPinned(noteId: Long, pinned: Boolean): Boolean =
        attempt("set pinned") { noteDao.setPinned(noteId, pinned) }

    suspend fun clearReminder(noteId: Long): Boolean =
        attempt("clear reminder") { noteDao.clearReminder(noteId) }

    /** Every note, for writing a backup. Null if the database couldn't be read. */
    suspend fun allNotesOnce(): List<Note>? = attempt("read notes", null) { noteDao.getAllNotesOnce() }

    /** Null if the import failed, in which case nothing was changed. */
    suspend fun importNotes(notes: List<Note>, now: Long, keepReminders: Boolean): ImportResult? =
        attempt("import notes", null) { noteDao.importNotes(notes, now, keepReminders) }

    suspend fun delete(note: Note): Boolean = deleteById(note.id)

    suspend fun deleteById(noteId: Long): Boolean = attempt("delete note") { noteDao.deleteNoteById(noteId) }

    private suspend fun attempt(what: String, block: suspend () -> Unit): Boolean =
        attempt(what, false) { block(); true }

    private suspend fun <T> attempt(what: String, onFailure: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            // Cancellation is not a database failure; swallowing it breaks structured concurrency.
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to $what", e)
            onFailure
        }
}
