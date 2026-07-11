package com.noty.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.noty.app.data.AppDatabase
import com.noty.app.data.Note

/**
 * Schedules one-shot reminder alarms via AlarmManager.
 *
 * Uses exact alarms when the user has granted SCHEDULE_EXACT_ALARM (granted by
 * default below Android 14), otherwise falls back to a windowed alarm so the
 * feature works without any permission round-trip.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.noty.app.ACTION_REMINDER"

    private const val WINDOW_MS = 10 * 60 * 1000L

    fun schedule(context: Context, note: Note) {
        val triggerAt = note.reminderAt ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context, note.id)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MS, pendingIntent)
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, noteId))
    }

    /**
     * Re-registers alarms for all pending reminders. Alarms don't survive a
     * reboot or an app update, so this runs from BootReceiver. Overdue
     * reminders fire immediately.
     */
    suspend fun rescheduleAll(context: Context) {
        val dao = AppDatabase.getDatabase(context).noteDao()
        val notificationHelper = NotificationHelper(context)
        val now = System.currentTimeMillis()

        dao.getReminderNotes().forEach { note ->
            val triggerAt = note.reminderAt ?: return@forEach
            if (triggerAt <= now) {
                notificationHelper.showReminderNotification(note)
                dao.updateNote(note.copy(reminderAt = null))
            } else {
                schedule(context, note)
            }
        }
    }

    private fun reminderPendingIntent(context: Context, noteId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            // Unique data URI distinguishes intents per note without request-code tricks
            data = Uri.parse("noty://reminder/$noteId")
            putExtra(NotificationHelper.EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context, noteId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
