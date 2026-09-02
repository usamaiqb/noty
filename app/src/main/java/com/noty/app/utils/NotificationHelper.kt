package com.noty.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "noty_persistent_channel"
        const val CHANNEL_ID_SERVICE = "noty_service_channel"
        const val CHANNEL_ID_REMINDERS = "noty_reminders_channel"
        const val ACTION_DELETE = "com.noty.app.ACTION_DELETE"
        const val ACTION_DISMISSED = "com.noty.app.ACTION_DISMISSED"
        const val ACTION_UNPIN = "com.noty.app.ACTION_UNPIN"
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Cache bitmaps to avoid recreation on each notification
    private val bitmapCache = mutableMapOf<Int, android.graphics.Bitmap>()

    private fun getIconBitmap(drawableId: Int): android.graphics.Bitmap? {
        bitmapCache[drawableId]?.let { return it }

        val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableId)
            ?: return null

        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) return null

        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmapCache[drawableId] = bitmap
        return bitmap
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for Notes
            val name = "Persistent Notes"
            val descriptionText = "Shows your active notes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)

            // Channel for Service (Minimized)
            val serviceName = "Noty Service"
            val serviceDescription = "Background service for monitoring notes"
            val serviceImportance = NotificationManager.IMPORTANCE_MIN
            val serviceChannel = NotificationChannel(CHANNEL_ID_SERVICE, serviceName, serviceImportance).apply {
                description = serviceDescription
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Channel for scheduled reminders (alerting)
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for note reminders"
            }
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    fun showNotification(note: Note) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // Use note.id as request code so each note has unique PendingIntent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, note.id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Using the new custom pen nib icon for the status bar
        val icon = R.drawable.ic_stat_noty

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(note.title)
            .setContentText(note.description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent(note.id))
            .setLocalOnly(true)

        if (note.isPinned) {
            builder.addAction(R.drawable.ic_unpin, "Unpin", unpinPendingIntent(note.id))
        }
        builder.addAction(R.drawable.ic_delete, "Delete", deletePendingIntent(note.id))

        notificationManager.notify(note.id.toInt(), builder.build())
    }

    fun createBaseNotification(title: String, content: String): android.app.Notification {
        val icon = R.drawable.ic_stat_noty
        return NotificationCompat.Builder(context, CHANNEL_ID_SERVICE)
            .setSmallIcon(icon)
            .setLargeIcon(getIconBitmap(R.drawable.ic_notification_large))
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    /**
     * Fires a note's reminder.
     *
     * A note has at most one notification at a time, posted under its own id.
     * For a pinned note this replaces the quiet persistent one instead of
     * adding a second entry: it alerts, then stays in the shade, because that
     * is what pinning means. An unpinned note gets a dismissible one-shot.
     */
    fun showReminderNotification(note: Note) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Distinct data URI keeps this PendingIntent separate from the pinned-note one
            data = android.net.Uri.parse("noty://reminder/${note.id}")
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, note.id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_noty)
            .setContentTitle(note.title)
            .setContentText(note.description ?: "Reminder")
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.description ?: "Reminder"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Pinning decides what happens after the alert: a pinned note stays
            // put and keeps its shade actions, an unpinned one is dismissible.
            .setOngoing(note.isPinned)
            .setAutoCancel(!note.isPinned)
            .setContentIntent(pendingIntent)

        if (note.isPinned) {
            builder.addAction(R.drawable.ic_unpin, "Unpin", unpinPendingIntent(note.id))
            builder.addAction(R.drawable.ic_delete, "Delete", deletePendingIntent(note.id))
            // Deliberately only for the pinned branch: ACTION_DISMISSED deletes
            // an unpinned note, so swiping away a fired one-shot must not fire it.
            builder.setDeleteIntent(dismissPendingIntent(note.id))
            // Updating a posted notification in place can suppress the heads-up
            // alert, so clear the quiet one before re-posting.
            notificationManager.cancel(note.id.toInt())
        }

        notificationManager.notify(note.id.toInt(), builder.build())
    }

    // Request-code offsets keep these distinct from the content intent and from
    // each other; they must stay stable so re-posting replaces rather than duplicates.
    private fun deletePendingIntent(noteId: Long): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context, -noteId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun dismissPendingIntent(noteId: Long): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISSED
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context, Int.MIN_VALUE / 2 + noteId.toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun unpinPendingIntent(noteId: Long): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_UNPIN
            putExtra(EXTRA_NOTE_ID, noteId)
        }
        return PendingIntent.getBroadcast(
            context, noteId.toInt() + 3000, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun cancelNotification(noteId: Int) {
        notificationManager.cancel(noteId)
    }

    fun cancelReminderNotification(noteId: Int) {
        // A note has one notification identity; kept as a named entry point so
        // call sites still read clearly.
        notificationManager.cancel(noteId)
    }

    fun syncNotifications(notes: List<Note>) {
        val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.map { it.id }.toSet()
        } else {
            emptySet()
        }

        notes.filter { it.isPinned }.forEach { note ->
            if (!activeNotifications.contains(note.id.toInt())) {
                showNotification(note)
            }
        }
    }
}
