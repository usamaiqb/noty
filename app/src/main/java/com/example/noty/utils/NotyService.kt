package com.example.noty.utils

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.noty.data.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NotyService : Service() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = Int.MAX_VALUE
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationHelper = NotificationHelper(applicationContext)

        // Start as foreground service with a low-priority notification
        val notification = notificationHelper.createBaseNotification(
            "Noty",
            "Keeping your notes visible"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }

        // Re-sync notifications to ensure consistency
        serviceScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val notes = database.noteDao().getAllNotes().first()
                notificationHelper.syncNotifications(notes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // START_STICKY ensures the OS tries to recreate the service if it's killed
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // When app is swiped from recents, re-sync notifications and restart service
        serviceScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val notificationHelper = NotificationHelper(applicationContext)
                val notes = database.noteDao().getAllNotes().first()
                notificationHelper.syncNotifications(notes)

                // Schedule service restart to ensure it keeps running
                if (notes.isNotEmpty()) {
                    val restartIntent = Intent(applicationContext, NotyService::class.java)
                    ContextCompat.startForegroundService(applicationContext, restartIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
