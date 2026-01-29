package com.example.noty.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.noty.R
import com.example.noty.data.Task
import com.example.noty.ui.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "noty_persistent_channel"
        const val CHANNEL_ID_SERVICE = "noty_service_channel"
        const val ACTION_DELETE = "com.example.noty.ACTION_DELETE"
        const val ACTION_DISMISSED = "com.example.noty.ACTION_DISMISSED"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun getIconBitmap(drawableId: Int): android.graphics.Bitmap {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableId)
        val bitmap = android.graphics.Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for Tasks
            val name = "Persistent Tasks"
            val descriptionText = "Shows your active cleaning tasks efficiently"
            val importance = NotificationManager.IMPORTANCE_LOW 
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)

            // Channel for Service (Minimized)
            val serviceName = "Noty Service"
            val serviceDescription = "Background service for monitoring tasks"
            val serviceImportance = NotificationManager.IMPORTANCE_MIN
            val serviceChannel = NotificationChannel(CHANNEL_ID_SERVICE, serviceName, serviceImportance).apply {
                description = serviceDescription
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun showNotification(task: Task) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DELETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val deletePendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, task.id.toInt(), deleteIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISSED
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val dismissPendingIntent: PendingIntent = PendingIntent.getBroadcast(
             context, task.id.toInt() + 10000, dismissIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Using the new custom pen nib icon for the status bar
        val icon = R.drawable.ic_stat_noty

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setLargeIcon(getIconBitmap(R.drawable.ic_notification_large))
            .setContentTitle(task.title)
            .setContentText(task.description)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent) // Triggered on swipe dismiss
            .addAction(android.R.drawable.ic_menu_delete, "Delete", deletePendingIntent)
            .setAutoCancel(false)

        notificationManager.notify(task.id.toInt(), builder.build())
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

    fun cancelNotification(taskId: Int) {
        notificationManager.cancel(taskId)
    }

    fun syncNotifications(tasks: List<Task>) {
        tasks.forEach { task ->
            showNotification(task)
        }
    }
}
