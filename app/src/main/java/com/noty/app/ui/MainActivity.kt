package com.noty.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import com.noty.app.utils.QuickNoteTileService
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 101
    }

    // Compose-observable state for Quick Settings tile trigger
    private var addNoteFromTile by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            NotyViewModelFactory(application)
        )[NotyViewModel::class.java]

        checkNotificationPermission()

        if (intent?.action == QuickNoteTileService.ACTION_ADD_NOTE) {
            addNoteFromTile = true
        }

        setContent {
            val dynamicColors by viewModel.dynamicColorsFlow.collectAsState(initial = true)
            NotyTheme(dynamicColor = dynamicColors) {
                NotyApp(
                    viewModel = viewModel,
                    triggerAddNote = addNoteFromTile,
                    onAddNoteTriggered = { addNoteFromTile = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == QuickNoteTileService.ACTION_ADD_NOTE) {
            addNoteFromTile = true
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }
}
