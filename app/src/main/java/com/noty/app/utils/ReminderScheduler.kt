package com.noty.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.noty.app.data.AppDatabase
import com.noty.app.data.Note
import com.noty.app.data.NotyRepository
import com.noty.app.ui.MainActivity

/**
 * Schedules a note's pin for later: one-shot alarms via AlarmManager that pin the note when
 * they fire. (Named for reminders, which scheduling replaced; the alarm side didn't change.)
 *
 * Scheduling requires SCHEDULE_EXACT_ALARM. When it isn't granted the feature is
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
            data = "package:${context.packageName}".toUri()
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
     * A scheduled note's time has come: pin it, and post it once with an alert. From then on
     * it is an ordinary pinned note. Shared by the alarm and by [rescheduleAll] for overdue ones.
     */
    suspend fun fire(context: Context, noteId: Long) {
        val dao = AppDatabase.getDatabase(context).noteDao()
        // Re-read at fire time: the schedule may have been removed, or the note deleted.
        val note = dao.getNoteById(noteId) ?: return
        if (note.reminderAt == null) return
        // setPinned also clears the time; a note is pinned or scheduled, never both.
        if (!NotyRepository(dao).setPinned(note.id, true)) return

        NotificationHelper(context).showNotification(note.copy(isPinned = true, reminderAt = null), alert = true)
        // The service keeps pinned notes posted, and the app may not be running to start it.
        // Alarm-clock alarms, boot and the exact-alarm grant may all start it from the
        // background; if a device refuses anyway, the note is already posted.
        try {
            ContextCompat.startForegroundService(context, Intent(context, NotyService::class.java))
        } catch (e: IllegalStateException) {
            Log.w("ReminderScheduler", "Couldn't start the service after pinning a scheduled note", e)
        }
    }

    /**
     * Re-registers alarms for all pending schedules. Alarms are cleared on
     * reboot, on app update, and when the exact-alarm permission is revoked and
     * re-granted. Overdue reminders fire immediately.
     */
    suspend fun rescheduleAll(context: Context) {
        if (!canScheduleExact(context)) return

        val dao = AppDatabase.getDatabase(context).noteDao()
        val now = System.currentTimeMillis()

        dao.getReminderNotes().forEach { note ->
            val triggerAt = note.reminderAt ?: return@forEach
            if (triggerAt <= now) fire(context, note.id) else schedule(context, note)
        }
    }

    private fun reminderPendingIntent(context: Context, noteId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            // Unique data URI distinguishes intents per note without request-code tricks
            data = "noty://reminder/$noteId".toUri()
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
            data = "noty://reminder/$noteId".toUri()
        }
        return PendingIntent.getActivity(
            context, noteId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
