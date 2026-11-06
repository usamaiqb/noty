package com.noty.app.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Whether Noty can do its job: show notes in the status bar.
 *
 * The single source for that answer. The home banner, the pin controls and the Settings
 * screen all read it, so they cannot disagree about whether pinning works.
 */
object NotificationAccess {

    /**
     * True when the app's notifications are on (on Android 13+ that includes the
     * POST_NOTIFICATIONS permission) and the Notes channel has not been blocked on its own.
     */
    fun canPostNotes(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        // A channel that does not exist yet is created at its default importance on first use.
        val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** The system screen that fixes whichever of the two is off. */
    fun settingsIntent(context: Context): Intent {
        val appNotificationsOff = !NotificationManagerCompat.from(context).areNotificationsEnabled()
        return if (appNotificationsOff) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
        }
    }
}
