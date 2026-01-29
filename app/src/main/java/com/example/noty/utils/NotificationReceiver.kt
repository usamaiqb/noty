package com.example.noty.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.noty.data.AppDatabase
import com.example.noty.data.NotyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.ACTION_DELETE) {
            val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1)
            if (taskId != -1L) {
                // Perform delete in background
                val database = AppDatabase.getDatabase(context)
                val repository = NotyRepository(database.taskDao())
                val notificationHelper = NotificationHelper(context)
                
                CoroutineScope(Dispatchers.IO).launch {
                    repository.deleteById(taskId)
                    notificationHelper.cancelNotification(taskId.toInt())
                }
            }
        } else if (intent.action == NotificationHelper.ACTION_DISMISSED) {
            val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1)
            if (taskId != -1L) {
                 val database = AppDatabase.getDatabase(context)
                 val notificationHelper = NotificationHelper(context)
                 
                 CoroutineScope(Dispatchers.IO).launch {
                     val task = database.taskDao().getTaskById(taskId)
                     if (task != null) {
                         // Resurrect the notification
                         notificationHelper.showNotification(task)
                     }
                 }
            }
        }
    }
}
