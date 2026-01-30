package com.example.noty.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.noty.R
import com.example.noty.data.Note
import com.example.noty.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "noty_persistent_channel"
        const val CHANNEL_ID_SERVICE = "noty_service_channel"
        const val ACTION_DELETE = "com.example.noty.ACTION_DELETE"
        const val ACTION_DISMISSED = "com.example.noty.ACTION_DISMISSED"
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

        val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_NOTE_ID, note.id)
        }
        // Use negative offset for delete intent to avoid collision with activity intent
        val deletePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, -note.id.toInt(), deleteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val dismissIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISSED
            putExtra(EXTRA_NOTE_ID, note.id)
        }
        // Use Int.MIN_VALUE/2 offset to avoid collision with other intents
        val dismissPendingIntent: PendingIntent = PendingIntent.getBroadcast(
             context, Int.MIN_VALUE / 2 + note.id.toInt(), dismissIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
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
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(R.drawable.ic_delete, "Delete", deletePendingIntent)
            .setAutoCancel(false)
            .setLocalOnly(true)

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

    fun cancelNotification(noteId: Int) {
        notificationManager.cancel(noteId)
    }

    fun syncNotifications(notes: List<Note>) {
        val activeNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.map { it.id }.toSet()
        } else {
            emptySet()
        }

        notes.forEach { note ->
            if (!activeNotifications.contains(note.id.toInt())) {
                showNotification(note)
            }
        }
    }
}
