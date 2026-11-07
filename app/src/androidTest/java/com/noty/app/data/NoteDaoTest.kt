package com.noty.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import com.noty.app.data.backup.NoteBackup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    // A fixed uuid per title, so two calls describe the same note and compare equal.
    private fun note(title: String, createdAt: Long, updatedAt: Long = createdAt) = Note(
        uuid = "uuid-$title",
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

    @Test
    fun uuid_isUnique() {
        runBlocking { dao.insertNote(note("Same", createdAt = 1_000L)) }

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insertNote(note("Same", createdAt = 2_000L)) }
        }
    }

    @Test
    fun importNotes_addsNew_replacesOlder_keepsNewerLocal() = runBlocking {
        val olderHere = dao.insertNote(note("Older here", createdAt = 1_000L, updatedAt = 1_000L))
        val newerHere = dao.insertNote(note("Newer here", createdAt = 1_000L, updatedAt = 5_000L))
        val backup = NoteBackup.decode(
            NoteBackup.encode(
                listOf(
                    note("Older here", createdAt = 1_000L, updatedAt = 3_000L).copy(description = "From file"),
                    note("Newer here", createdAt = 1_000L, updatedAt = 2_000L).copy(description = "From file"),
                    note("Only in file", createdAt = 1_000L, updatedAt = 2_000L)
                )
            )
        )

        val result = dao.importNotes(backup, now = 0L, keepReminders = true)

        assertEquals(1, result.unchanged)
        assertEquals(setOf("Older here", "Only in file"), result.changedNotes.map { it.title }.toSet())
        assertEquals("From file", dao.getNoteById(olderHere)!!.description)
        assertNull(dao.getNoteById(newerHere)!!.description)
        assertEquals(3, dao.getAllNotesOnce().size)
        // Every changed note carries the row id it is stored under.
        result.changedNotes.forEach { assertEquals(it, dao.getNoteById(it.id)) }
    }

    @Test
    fun importNotes_sameFileTwice_changesNothing() = runBlocking {
        val backup = NoteBackup.decode(NoteBackup.encode(listOf(note("A", 1_000L), note("B", 2_000L))))
        dao.importNotes(backup, now = 0L, keepReminders = true)

        val second = dao.importNotes(backup, now = 0L, keepReminders = true)

        assertEquals(emptyList<Note>(), second.changedNotes)
        assertEquals(2, second.unchanged)
        assertEquals(2, dao.getAllNotesOnce().size)
    }
}
