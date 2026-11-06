package com.noty.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noty.app.data.AppDatabase
import com.noty.app.data.NotyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val noteId = intent.getLongExtra(NotificationHelper.EXTRA_NOTE_ID, -1)
                if (noteId == -1L) return@launch

                val database = AppDatabase.getDatabase(context)
                val notificationHelper = NotificationHelper(context)

                when (intent.action) {
                    NotificationHelper.ACTION_DELETE -> {
                        val repository = NotyRepository(database.noteDao())
                        repository.deleteById(noteId)
                        notificationHelper.cancelNotification(noteId.toInt())
                        // Otherwise the alarm outlives the note until it fires
                        ReminderScheduler.cancel(context, noteId)
                    }
                    NotificationHelper.ACTION_DISMISSED -> {
                        // A pinned note's notification comes straight back. Nothing else
                        // happens: dismissing a notification must never delete a note.
                        val note = database.noteDao().getNoteById(noteId)
                        if (note != null && note.isPinned) {
                            notificationHelper.showNotification(note)
                        }
                    }
                    NotificationHelper.ACTION_UNPIN -> {
                        // Mark as unpinned: the note stays in the DB, its notification goes
                        if (NotyRepository(database.noteDao()).setPinned(noteId, false)) {
                            notificationHelper.cancelNotification(noteId.toInt())
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
