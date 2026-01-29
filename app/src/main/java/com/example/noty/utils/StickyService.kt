package com.example.noty.utils

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

import com.example.noty.data.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

class StickyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground immediately
        startForegroundService()

        // Sync notifications
        serviceScope.launch(Dispatchers.IO) {
            val database = com.example.noty.data.AppDatabase.getDatabase(applicationContext)
            val notificationHelper = NotificationHelper(applicationContext)
            
            database.taskDao().getAllTasks().first().let { tasksList ->
                notificationHelper.syncNotifications(tasksList)
            }
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationHelper = NotificationHelper(this)
        // We need a base notification for the service itself
        //Ideally, this would summarize "X active tasks"
        val notification = notificationHelper.createBaseNotification("Noty Service", "Running in background")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                1001, // ID for the service notification
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
            )
        } else {
            startForeground(1001, notification)
        }
    }
}
