package com.noty.app.utils

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.noty.app.R
import com.noty.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class NotyService : Service() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = Int.MAX_VALUE
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isObservingNotes = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationHelper = NotificationHelper(applicationContext)

        // Start as foreground service with a low-priority notification
        val notification = notificationHelper.createBaseNotification(
            getString(R.string.app_name),
            getString(R.string.service_notification_text)
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

        // Observe notes and stop self when no pinned notes remain.
        // Guards against the case where the ViewModel is not alive (e.g. boot-only start,
        // or the user deleted the last pinned note via a notification action).
        if (!isObservingNotes) {
            isObservingNotes = true
            serviceScope.launch(Dispatchers.IO) {
                try {
                    AppDatabase.getDatabase(applicationContext)
                        .noteDao()
                        .getAllNotes()
                        .collect { notes ->
                            if (notes.none { it.isPinned }) {
                                stopSelf()
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
                if (notes.any { it.isPinned }) {
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
        isObservingNotes = false
        serviceScope.cancel()
    }
}
