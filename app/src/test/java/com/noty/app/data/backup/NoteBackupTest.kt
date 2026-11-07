package com.noty.app.data.backup

import com.noty.app.data.Note
import com.noty.app.data.NoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBackupTest {

    private val notes = listOf(
        Note(
            id = 7,
            uuid = "0b6f2c3e-8d1a-4c5e-9f2b-1a2b3c4d5e6f",
            title = "Groceries",
            description = "Milk, \"eggs\"\nbread",
            type = NoteType.WORK,
            createdAt = 1_000L,
            updatedAt = 2_000L,
            isPinned = true,
            reminderAt = 3_000L
        ),
        Note(
            id = 9,
            uuid = "5c1d7e9a-2b3c-4d5e-8f6a-7b8c9d0e1f2a",
            title = "Plain",
            type = NoteType.NOTE,
            createdAt = 4_000L,
            isPinned = false
        )
    )

    @Test
    fun roundTrip_keepsEverythingButTheRowId() {
        val decoded = NoteBackup.decode(NoteBackup.encode(notes, exportedAt = 5_000L))

        assertEquals(notes.map { it.copy(id = 0) }, decoded)
    }

    @Test
    fun encode_writesFormatAndVersion_andNoRowIds() {
        val text = NoteBackup.encode(notes, exportedAt = 5_000L)

        assertTrue(text.contains("\"format\": \"noty-backup\""))
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(!text.contains("\"id\""))
    }

    @Test
    fun decode_minimalNote_fillsDefaults() {
        val decoded = NoteBackup.decode(
            """{"format":"noty-backup","version":1,"notes":[{"uuid":"a","title":"T","createdAt":10}]}"""
        ).single()

        assertEquals("a", decoded.uuid)
        assertNull(decoded.description)
        assertEquals(NoteType.NOTE, decoded.type)
        assertEquals(10L, decoded.updatedAt)
        assertEquals(false, decoded.isPinned)
        assertNull(decoded.reminderAt)
    }

    @Test
    fun decode_unknownFieldsAndType_fromANewerMinorChange_stillRead() {
        val decoded = NoteBackup.decode(
            """{"format":"noty-backup","version":1,"extra":true,
               "notes":[{"uuid":"a","title":"T","createdAt":10,"type":"CHECKLIST","color":"red"}]}"""
        ).single()

        assertEquals(NoteType.NOTE, decoded.type)
    }

    @Test
    fun decode_newerVersion_isRejectedAsNewer() {
        val e = assertThrows(InvalidBackupException::class.java) {
            NoteBackup.decode("""{"format":"noty-backup","version":2,"notes":[]}""")
        }
        assertEquals(InvalidBackupException.Reason.NEWER_VERSION, e.reason)
    }

    @Test
    fun decode_notABackup_isRejected() {
        listOf(
            "",
            "hello",
            "[]",
            """{"format":"something-else","version":1,"notes":[]}""",
            """{"format":"noty-backup","version":1,"notes":[{"title":"No uuid","createdAt":1}]}""",
            """{"format":"noty-backup","version":1,"notes":[{"uuid":" ","title":"Blank","createdAt":1}]}"""
        ).forEach { text ->
            val e = assertThrows(text, InvalidBackupException::class.java) { NoteBackup.decode(text) }
            assertEquals(text, InvalidBackupException.Reason.NOT_A_BACKUP, e.reason)
        }
    }
}
