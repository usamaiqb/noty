package com.noty.app.data.backup

import com.noty.app.data.Note
import com.noty.app.data.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportPlanTest {

    private val now = 10_000L

    private fun note(uuid: String, updatedAt: Long, id: Long = 0, reminderAt: Long? = null) = Note(
        id = id,
        uuid = uuid,
        title = "Note $uuid at $updatedAt",
        type = NoteType.NOTE,
        createdAt = 1_000L,
        updatedAt = updatedAt,
        reminderAt = reminderAt
    )

    private fun plan(incoming: List<Note>, existing: List<Note>, keepReminders: Boolean = true) =
        ImportPlan.of(incoming, existing, now, keepReminders)

    @Test
    fun newNote_isInsertedWithoutARowId() {
        val result = plan(listOf(note("a", 2_000L, id = 42)), existing = emptyList())

        assertEquals(listOf(note("a", 2_000L)), result.toInsert)
        assertEquals(emptyList<Note>(), result.toReplace)
        assertEquals(0, result.unchanged)
    }

    @Test
    fun newerCopy_replacesTheLocalNote_keepingItsRowId() {
        val result = plan(listOf(note("a", 3_000L)), existing = listOf(note("a", 2_000L, id = 5)))

        assertEquals(listOf(note("a", 3_000L, id = 5)), result.toReplace)
        assertEquals(emptyList<Note>(), result.toInsert)
    }

    @Test
    fun olderOrSameCopy_leavesTheLocalNote() {
        val existing = listOf(note("a", 2_000L, id = 5), note("b", 2_000L, id = 6))

        val result = plan(listOf(note("a", 1_500L), note("b", 2_000L)), existing)

        assertEquals(emptyList<Note>(), result.toInsert)
        assertEquals(emptyList<Note>(), result.toReplace)
        assertEquals(2, result.unchanged)
    }

    @Test
    fun sameUuidTwiceInTheFile_latestEditWins() {
        val result = plan(listOf(note("a", 2_000L), note("a", 4_000L), note("a", 3_000L)), emptyList())

        assertEquals(listOf(note("a", 4_000L)), result.toInsert)
        assertEquals(2, result.unchanged)
    }

    @Test
    fun pastReminder_isDropped_futureOneKept() {
        val result = plan(
            listOf(note("past", 2_000L, reminderAt = now - 1), note("future", 2_000L, reminderAt = now + 1)),
            existing = emptyList()
        )

        assertNull(result.toInsert.single { it.uuid == "past" }.reminderAt)
        assertEquals(now + 1, result.toInsert.single { it.uuid == "future" }.reminderAt)
    }

    @Test
    fun withoutExactAlarms_remindersAreDropped() {
        val result = plan(listOf(note("a", 2_000L, reminderAt = now + 1)), emptyList(), keepReminders = false)

        assertNull(result.toInsert.single().reminderAt)
    }
}
