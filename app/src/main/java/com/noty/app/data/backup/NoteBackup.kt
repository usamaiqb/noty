package com.noty.app.data.backup

import com.noty.app.data.Note
import com.noty.app.data.NoteType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The backup file: plain JSON, so it stays readable and can be checked by hand.
 *
 * The layout is versioned on its own, separately from the database. A database migration
 * doesn't have to change the file, and a file from an older app must keep importing.
 * Only raise [VERSION] for a change that older apps can't read correctly, and keep reading
 * every earlier version.
 */
object NoteBackup {
    const val FORMAT = "noty-backup"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"

    private val json = Json {
        prettyPrint = true
        // A later app may add fields; this one keeps what it understands.
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(notes: List<Note>, exportedAt: Long = System.currentTimeMillis()): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                format = FORMAT,
                version = VERSION,
                exportedAt = exportedAt,
                notes = notes.map(BackupNote::from)
            )
        )

    /**
     * Reads a backup into notes that are ready to merge. Their `id` is 0: row ids belong to
     * one device, so a note is recognised by its uuid instead.
     *
     * @throws InvalidBackupException if the text isn't a backup this app can read.
     */
    fun decode(text: String): List<Note> {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: IllegalArgumentException) {
            // SerializationException (not JSON, or not this shape) is one of these too.
            throw InvalidBackupException(InvalidBackupException.Reason.NOT_A_BACKUP, e)
        }
        val problem = when {
            file.format != FORMAT -> InvalidBackupException.Reason.NOT_A_BACKUP
            file.version > VERSION -> InvalidBackupException.Reason.NEWER_VERSION
            file.notes.any { it.uuid.isBlank() } -> InvalidBackupException.Reason.NOT_A_BACKUP
            else -> null
        }
        if (problem != null) throw InvalidBackupException(problem)
        return file.notes.map(BackupNote::toNote)
    }
}

class InvalidBackupException(val reason: Reason, cause: Throwable? = null) :
    Exception("Not a readable Noty backup: $reason", cause) {
    enum class Reason { NOT_A_BACKUP, NEWER_VERSION }
}

@Serializable
private data class BackupFile(
    val format: String,
    val version: Int,
    val exportedAt: Long = 0,
    val notes: List<BackupNote> = emptyList()
)

/**
 * One note as it is written to the file. Kept apart from [Note] so that a schema change
 * can't silently change the file format.
 */
@Serializable
private data class BackupNote(
    val uuid: String,
    val title: String,
    val description: String? = null,
    // Written by name. An unknown name (from a newer app) reads back as a plain note.
    val type: String = NoteType.NOTE.name,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    @SerialName("pinned") val isPinned: Boolean = false,
    val reminderAt: Long? = null
) {
    fun toNote() = Note(
        uuid = uuid,
        title = title,
        description = description,
        type = NoteType.entries.firstOrNull { it.name == type } ?: NoteType.NOTE,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        reminderAt = reminderAt
    )

    companion object {
        fun from(note: Note) = BackupNote(
            uuid = note.uuid,
            title = note.title,
            description = note.description,
            type = note.type.name,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            isPinned = note.isPinned,
            reminderAt = note.reminderAt
        )
    }
}
