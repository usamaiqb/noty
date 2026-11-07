package com.noty.app.data.backup

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.io.Reader

/**
 * Reads and writes backup files the user picked in the system file picker. Blocking: call
 * from an IO dispatcher.
 */
object BackupFiles {
    private const val TAG = "BackupFiles"

    // Far beyond any real backup (a note is a few hundred characters), and small enough to
    // hold in memory safely.
    private const val MAX_CHARS = 10_000_000

    /** @return false if the file couldn't be written. */
    fun write(resolver: ContentResolver, uri: Uri, text: String): Boolean = try {
        // "wt" truncates: overwriting a longer file must not leave its tail behind.
        resolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } != null
    } catch (e: IOException) {
        Log.e(TAG, "Failed to write backup", e)
        false
    } catch (e: SecurityException) {
        Log.e(TAG, "No access to the backup file", e)
        false
    }

    /**
     * The file's text, or null if it can't be read. Capped, so that picking the wrong file,
     * such as a video, fails cleanly instead of loading it all into memory.
     */
    fun read(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.openInputStream(uri)?.bufferedReader()?.use { readCapped(it, MAX_CHARS) }
    } catch (e: IOException) {
        Log.e(TAG, "Failed to read backup", e)
        null
    } catch (e: SecurityException) {
        Log.e(TAG, "No access to the backup file", e)
        null
    }

    /** The reader's text, or null as soon as it runs past [maxChars]. */
    internal fun readCapped(reader: Reader, maxChars: Int): String? {
        val text = StringBuilder()
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        var read = reader.read(buffer)
        while (read >= 0) {
            text.appendRange(buffer, 0, read)
            if (text.length > maxChars) return null
            read = reader.read(buffer)
        }
        return text.toString()
    }
}
