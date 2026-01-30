package com.example.noty.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.noty.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            val database = AppDatabase.getDatabase(context)
            val notificationHelper = NotificationHelper(context)

            // We use a coroutine to perform the database operation asynchronously
            // and keep the receiver alive until it completes.
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    // Sync notifications directly
                    val notesList = database.noteDao().getAllNotes().first()
                    notificationHelper.syncNotifications(notesList)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
