package com.noty.app.ui

import android.app.Application
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noty.app.R
import com.noty.app.data.AppDatabase
import com.noty.app.data.NotyRepository
import com.noty.app.data.backup.BackupFiles
import com.noty.app.data.backup.InvalidBackupException
import com.noty.app.data.backup.NoteBackup
import com.noty.app.utils.NotificationHelper
import com.noty.app.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Why an export or import didn't go through. */
enum class BackupMessage(@StringRes private val res: Int) : UserMessage {
    EXPORT_FAILED(R.string.message_export_failed),
    IMPORT_FAILED(R.string.message_import_failed),
    IMPORT_NOT_A_BACKUP(R.string.message_import_not_a_backup),
    IMPORT_NEWER_VERSION(R.string.message_import_newer_version),
    IMPORT_NOTHING_NEW(R.string.message_import_nothing_new);

    override fun text(resources: Resources): String = resources.getString(res)
}

/** A result that reports how many notes it covered. */
data class CountMessage(@PluralsRes private val res: Int, private val count: Int) : UserMessage {
    override fun text(resources: Resources): String = resources.getQuantityString(res, count, count)
}

/**
 * Export and import of notes, started from Settings. Its own ViewModel so a long import
 * outlives the screen and keeps running if the user leaves Settings.
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "BackupViewModel"
    }

    private val repository = NotyRepository(AppDatabase.getDatabase(application).noteDao())
    private val notificationHelper = NotificationHelper(application)

    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    /** Writes every note to [uri], a file the user picked in the system file picker. */
    fun exportNotes(uri: Uri) = viewModelScope.launch {
        val notes = repository.allNotesOnce()
        if (notes == null) {
            _messages.send(BackupMessage.EXPORT_FAILED)
            return@launch
        }
        val text = NoteBackup.encode(notes)
        val written = withContext(Dispatchers.IO) {
            BackupFiles.write(getApplication<Application>().contentResolver, uri, text)
        }
        _messages.send(
            if (written) CountMessage(R.plurals.message_exported, notes.size) else BackupMessage.EXPORT_FAILED
        )
    }

    /**
     * Merges the backup at [uri] into the notes here: new notes are added, and a note that is
     * already here is replaced only by a more recently edited copy. See ImportPlan.
     */
    fun importNotes(uri: Uri) = viewModelScope.launch {
        val app = getApplication<Application>()
        val text = withContext(Dispatchers.IO) { BackupFiles.read(app.contentResolver, uri) }
        if (text == null) {
            _messages.send(BackupMessage.IMPORT_FAILED)
            return@launch
        }
        val notes = try {
            withContext(Dispatchers.Default) { NoteBackup.decode(text) }
        } catch (e: InvalidBackupException) {
            Log.w(TAG, "Rejected backup file", e)
            _messages.send(
                when (e.reason) {
                    InvalidBackupException.Reason.NOT_A_BACKUP -> BackupMessage.IMPORT_NOT_A_BACKUP
                    InvalidBackupException.Reason.NEWER_VERSION -> BackupMessage.IMPORT_NEWER_VERSION
                }
            )
            return@launch
        }

        val result = repository.importNotes(
            notes,
            now = System.currentTimeMillis(),
            keepReminders = ReminderScheduler.canScheduleExact(app)
        )
        if (result == null) {
            _messages.send(BackupMessage.IMPORT_FAILED)
            return@launch
        }

        // Bring the shade and the alarms in line with what is now stored.
        result.changedNotes.forEach { note ->
            if (note.isPinned) {
                notificationHelper.showNotification(note)
            } else {
                notificationHelper.cancelNotification(note.id.toInt())
            }
            if (note.reminderAt == null) {
                ReminderScheduler.cancel(app, note.id)
            } else if (!ReminderScheduler.schedule(app, note)) {
                repository.clearReminder(note.id)
            }
        }
        _messages.send(
            if (result.changedNotes.isEmpty()) {
                BackupMessage.IMPORT_NOTHING_NEW
            } else {
                CountMessage(R.plurals.message_imported, result.changedNotes.size)
            }
        )
    }
}
