package com.noty.app.ui

import com.noty.app.data.Note
import com.noty.app.data.NoteType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsavedChangesTest {

    private val note = Note(
        id = 1,
        title = "Title",
        description = "Text",
        type = NoteType.NOTE,
        isPinned = true,
        reminderAt = 5_000L
    )

    private fun edited(
        title: String = "Title",
        description: String = "Text",
        isPinned: Boolean = true,
        reminderAt: Long? = 5_000L
    ) = hasUnsavedChanges(note, title, description, isPinned, reminderAt)

    @Test
    fun newNote_empty_hasNoChanges() {
        assertFalse(hasUnsavedChanges(null, "", "", isPinned = true, reminderAt = null))
    }

    @Test
    fun newNote_whitespaceOnly_hasNoChanges() {
        assertFalse(hasUnsavedChanges(null, "  ", "\n", isPinned = true, reminderAt = null))
    }

    @Test
    fun newNote_pinSwitchAlone_hasNoChanges() {
        assertFalse(hasUnsavedChanges(null, "", "", isPinned = false, reminderAt = null))
    }

    @Test
    fun newNote_title_hasChanges() {
        assertTrue(hasUnsavedChanges(null, "Buy milk", "", isPinned = true, reminderAt = null))
    }

    @Test
    fun newNote_descriptionOnly_hasChanges() {
        assertTrue(hasUnsavedChanges(null, "", "Six minutes of typing", isPinned = true, reminderAt = null))
    }

    @Test
    fun newNote_reminderOnly_hasChanges() {
        assertTrue(hasUnsavedChanges(null, "", "", isPinned = true, reminderAt = 9_000L))
    }

    @Test
    fun edit_untouched_hasNoChanges() {
        assertFalse(edited())
    }

    @Test
    fun edit_trailingSpace_hasNoChanges() {
        assertFalse(edited(title = "Title ", description = " Text"))
    }

    @Test
    fun edit_noDescription_emptyField_hasNoChanges() {
        val bare = note.copy(description = null)
        assertFalse(hasUnsavedChanges(bare, "Title", "", isPinned = true, reminderAt = 5_000L))
    }

    @Test
    fun edit_eachField_hasChanges() {
        assertTrue(edited(title = "Other"))
        assertTrue(edited(description = "Other"))
        assertTrue(edited(isPinned = false))
        assertTrue(edited(reminderAt = null))
    }
}
