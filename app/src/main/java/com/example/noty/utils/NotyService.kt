package com.example.noty.utils

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.noty.data.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class NotyService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Re-sync notifications to ensure consistency
        serviceScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                val notificationHelper = NotificationHelper(applicationContext)
                val notes = database.noteDao().getAllNotes().first()
                notificationHelper.syncNotifications(notes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // START_STICKY ensures the OS tries to recreate the service if it's killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
