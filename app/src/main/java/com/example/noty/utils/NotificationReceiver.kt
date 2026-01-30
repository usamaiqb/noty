package com.example.noty.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.noty.data.AppDatabase
import com.example.noty.data.NotyRepository
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
                    }
                    NotificationHelper.ACTION_DISMISSED -> {
                        val note = database.noteDao().getNoteById(noteId)
                        if (note != null) {
                            // Resurrect the notification because user wants them persistent
                            notificationHelper.showNotification(note)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
