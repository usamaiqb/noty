package com.example.noty.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class NotyRepository(private val noteDao: NoteDao) {

    companion object {
        private const val TAG = "NotyRepository"
    }

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insert(note: Note): Long {
        return try {
            noteDao.insertNote(note)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert note", e)
            -1L
        }
    }

    suspend fun delete(note: Note): Boolean {
        return try {
            noteDao.deleteNoteById(note.id)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete note", e)
            false
        }
    }

    suspend fun deleteById(noteId: Long): Boolean {
        return try {
            noteDao.deleteNoteById(noteId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete note by id", e)
            false
        }
    }
}
