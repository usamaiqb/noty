package com.noty.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

class BackupFilesTest {

    @Test
    fun readCapped_withinLimit_returnsAllText() {
        val text = "x".repeat(DEFAULT_BUFFER_SIZE * 3 + 7)

        assertEquals(text, BackupFiles.readCapped(StringReader(text), maxChars = text.length))
    }

    @Test
    fun readCapped_overLimit_returnsNull() {
        val text = "x".repeat(DEFAULT_BUFFER_SIZE * 3)

        assertNull(BackupFiles.readCapped(StringReader(text), maxChars = text.length - 1))
    }
}
