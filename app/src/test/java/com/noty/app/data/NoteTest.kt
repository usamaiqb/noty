package com.noty.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class NoteTest {

    private val note = Note(
        id = 1,
        title = "Title",
        description = "Text",
        type = NoteType.NOTE,
        createdAt = 1_000L
    )

    @Test
    fun newNote_updatedAtStartsAtCreatedAt() {
        assertEquals(1_000L, note.updatedAt)
    }

    @Test
    fun withText_unchanged_keepsUpdatedAt() {
        assertSame(note, note.withText("Title", "Text", now = 5_000L))
    }

    @Test
    fun withText_titleChanged_bumpsUpdatedAt() {
        val edited = note.withText("New title", "Text", now = 5_000L)
        assertEquals("New title", edited.title)
        assertEquals(5_000L, edited.updatedAt)
        assertEquals(1_000L, edited.createdAt)
    }

    @Test
    fun withText_descriptionCleared_bumpsUpdatedAt() {
        val edited = note.withText("Title", null, now = 5_000L)
        assertEquals(null, edited.description)
        assertEquals(5_000L, edited.updatedAt)
    }
}
