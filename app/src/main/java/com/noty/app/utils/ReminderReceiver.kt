package com.noty.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                if (noteId != -1L) ReminderScheduler.fire(context, noteId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
