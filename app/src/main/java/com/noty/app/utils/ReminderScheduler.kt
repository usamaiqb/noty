package com.noty.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.noty.app.data.AppDatabase
import com.noty.app.data.Note
import com.noty.app.ui.MainActivity

/**
 * Schedules one-shot reminder alarms via AlarmManager.
 *
 * Reminders require SCHEDULE_EXACT_ALARM. When it isn't granted the feature is
 * disabled in the UI rather than silently degraded — an inexact alarm can slip
 * by 10+ minutes, which is worse than not offering the feature at all.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.noty.app.ACTION_REMINDER"

    /** Whether the OS will let us deliver a reminder at the requested minute. */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /** Settings screen where the user grants "Alarms & reminders". */
    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /**
     * Registers the alarm. Returns false if the permission is missing, in which
     * case nothing is scheduled.
     */
    fun schedule(context: Context, note: Note): Boolean {
        val triggerAt = note.reminderAt ?: return false
        if (!canScheduleExact(context)) return false
        // A past trigger time makes setAlarmClock fire the instant it's set.
        // The pickers block this, but the user can still linger past their own
        // chosen time before saving.
        if (triggerAt <= System.currentTimeMillis()) return false

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // setAlarmClock is the only alarm exempt from Doze deferral, so it fires
        // on time even after the device has been idle for days.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAt, showIntent(context, note.id)),
            reminderPendingIntent(context, note.id)
        )
        return true
    }

    fun cancel(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, noteId))
    }

    /**
     * Re-registers alarms for all pending reminders. Alarms are cleared on
     * reboot, on app update, and when the exact-alarm permission is revoked and
     * re-granted. Overdue reminders fire immediately.
     */
    suspend fun rescheduleAll(context: Context) {
        if (!canScheduleExact(context)) return

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

    /** Tapping the status-bar alarm chip opens the note. */
    private fun showIntent(context: Context, noteId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("noty://reminder/$noteId")
        }
        return PendingIntent.getActivity(
            context, noteId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
