package com.noty.app.utils

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The system drops our exact alarms when the user revokes "Alarms & reminders".
 * Re-granting it doesn't bring them back, so re-register from the state-change
 * broadcast. Reminders stay in the database while revoked, so nothing is lost.
 */
@RequiresApi(Build.VERSION_CODES.S)
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        if (!ReminderScheduler.canScheduleExact(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
