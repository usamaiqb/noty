package com.noty.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noty.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val noteId = intent.getLongExtra(NotificationHelper.EXTRA_NOTE_ID, -1)
                if (noteId == -1L) return@launch

                val dao = AppDatabase.getDatabase(context).noteDao()
                val note = dao.getNoteById(noteId) ?: return@launch

                NotificationHelper(context).showReminderNotification(note)

                // One-shot: clear the reminder once it has fired
                if (note.reminderAt != null) {
                    dao.updateNote(note.copy(reminderAt = null))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
