package com.noty.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        dao = db.noteDao()
    }

    @After
    fun tearDown() = db.close()

    private fun note(title: String, createdAt: Long, updatedAt: Long = createdAt) = Note(
        title = title,
        type = NoteType.NOTE,
        createdAt = createdAt,
        updatedAt = updatedAt,
        reminderAt = 9_000L
    )

    @Test
    fun getAllNotes_lastEditedFirst() = runBlocking {
        dao.insertNote(note("Old, edited today", createdAt = 1_000L, updatedAt = 5_000L))
        dao.insertNote(note("Newer, never edited", createdAt = 3_000L))

        val titles = dao.getAllNotes().first().map { it.title }

        assertEquals(listOf("Old, edited today", "Newer, never edited"), titles)
    }

    @Test
    fun setPinned_changesOnlyThePin() = runBlocking {
        val id = dao.insertNote(note("Note", createdAt = 1_000L))

        dao.setPinned(id, false)

        val stored = dao.getNoteById(id)!!
        assertFalse(stored.isPinned)
        assertEquals(note("Note", createdAt = 1_000L).copy(id = id, isPinned = false), stored)
    }

    @Test
    fun clearReminder_changesOnlyTheReminder() = runBlocking {
        val id = dao.insertNote(note("Note", createdAt = 1_000L))

        dao.clearReminder(id)

        val stored = dao.getNoteById(id)!!
        assertNull(stored.reminderAt)
        assertEquals(note("Note", createdAt = 1_000L).copy(id = id, reminderAt = null), stored)
    }

    @Test
    fun setPinned_doesNotUndoAnEditMadeInTheMeantime() = runBlocking {
        val id = dao.insertNote(note("Before", createdAt = 1_000L))
        dao.updateNote(dao.getNoteById(id)!!.withText("After", null, now = 2_000L))

        dao.setPinned(id, false)

        assertEquals("After", dao.getNoteById(id)!!.title)
    }
}
