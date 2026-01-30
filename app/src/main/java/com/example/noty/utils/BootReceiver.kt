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
            // Start the foreground service on boot
            // The service will handle the sync once it starts
            val serviceIntent = Intent(context, NotyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
    }
}
