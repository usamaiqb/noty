package com.noty.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "noty_persistent_channel"
        const val CHANNEL_ID_SERVICE = "noty_service_channel"
        // Kept from when this channel was "Reminders", so the user's settings for it carry over.
        const val CHANNEL_ID_SCHEDULED = "noty_reminders_channel"
        const val ACTION_DELETE = "com.noty.app.ACTION_DELETE"
        const val ACTION_DISMISSED = "com.noty.app.ACTION_DISMISSED"
        const val ACTION_UNPIN = "com.noty.app.ACTION_UNPIN"
        const val EXTRA_NOTE_ID = "extra_note_id"

        // Request-code offsets keeping each note's PendingIntents distinct from one
        // another. Any collision would make one action overwrite another's intent.
        private const val REQUEST_CODE_UNPIN_OFFSET = 3000
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

        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
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
        // Channel for Notes
        val name = context.getString(R.string.channel_notes_name)
        val descriptionText = context.getString(R.string.channel_notes_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)

        // Channel for Service (Minimized)
        val serviceName = context.getString(R.string.channel_service_name)
        val serviceDescription = context.getString(R.string.channel_service_description)
        val serviceImportance = NotificationManager.IMPORTANCE_MIN
        val serviceChannel = NotificationChannel(CHANNEL_ID_SERVICE, serviceName, serviceImportance).apply {
            description = serviceDescription
        }
        notificationManager.createNotificationChannel(serviceChannel)

        // The moment a scheduled note arrives. Its own channel because the user controls each
        // channel's sound: a post can be quieter than its channel but never louder, and a user
        // who mutes the persistent "Notes" channel still wants to hear a scheduled note land.
        // Re-creating a channel with the same id renames it and keeps the user's settings.
        val scheduledChannel = NotificationChannel(
            CHANNEL_ID_SCHEDULED,
            context.getString(R.string.channel_scheduled_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_scheduled_description)
        }
        notificationManager.createNotificationChannel(scheduledChannel)
    }

    /**
     * Posts a pinned note's notification, the only one a note ever has, under its own id.
     *
     * [alert] is for the moment a scheduled note arrives: it posts on the alerting channel
     * so it sounds and peeks once. Every later re-post (an edit, a sync) is quiet, on "Notes".
     */
    fun showNotification(note: Note, alert: Boolean = false) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // Use note.id as request code so each note has unique PendingIntent
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, note.id.toInt(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Using the new custom pen nib icon for the status bar
        val icon = R.drawable.ic_stat_noty

        val builder = NotificationCompat.Builder(context, if (alert) CHANNEL_ID_SCHEDULED else CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(note.title)
            .setContentText(note.description)
            .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent(note.id))
            .setLocalOnly(true)

        // Without BigTextStyle the shade keeps only the first line of the
        // description, dropping line breaks even when expanded.
        if (!note.description.isNullOrBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(note.description))
        }

        if (note.isPinned) {
            builder.addAction(
                R.drawable.ic_unpin,
                context.getString(R.string.action_unpin),
                unpinPendingIntent(note.id)
            )
        }
        builder.addAction(
            R.drawable.ic_delete,
            context.getString(R.string.action_delete),
            deletePendingIntent(note.id)
        )

        // Updating a posted notification in place can suppress the heads-up alert (and
        // setOnlyAlertOnce silences updates), so clear the quiet one first.
        if (alert) notificationManager.cancel(note.id.toInt())
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
            context, noteId.toInt() + REQUEST_CODE_UNPIN_OFFSET, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun cancelNotification(noteId: Int) {
        notificationManager.cancel(noteId)
    }

    fun syncNotifications(notes: List<Note>) {
        val activeNotifications = notificationManager.activeNotifications.map { it.id }.toSet()

        notes.filter { it.isPinned }.forEach { note ->
            if (!activeNotifications.contains(note.id.toInt())) {
                showNotification(note)
            }
        }
    }
}
