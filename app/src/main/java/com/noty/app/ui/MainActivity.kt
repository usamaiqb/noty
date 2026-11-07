package com.noty.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noty.app.ui.theme.NotyTheme
import com.noty.app.utils.QuickNoteTileService
import com.noty.app.utils.ThemeManager

class MainActivity : ComponentActivity() {

    // The answer needs no handling here: NotyApp rechecks notification access whenever the
    // app resumes, which includes the moment this prompt closes.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Compose-observable state for Quick Settings tile trigger
    private var addNoteFromTile by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(
            this,
            NotyViewModelFactory(application)
        )[NotyViewModel::class.java]
        // The default factory builds an AndroidViewModel from the Application on its own.
        val backupViewModel = ViewModelProvider(this)[BackupViewModel::class.java]

        checkNotificationPermission()

        if (intent?.action == QuickNoteTileService.ACTION_ADD_NOTE) {
            addNoteFromTile = true
        }

        setContent {
            val dynamicColors by viewModel.dynamicColorsFlow.collectAsStateWithLifecycle(true)
            val themeMode by viewModel.themeFlow
                .collectAsStateWithLifecycle(ThemeManager.ThemeMode.SYSTEM)
            NotyTheme(themeMode = themeMode, dynamicColor = dynamicColors) {
                NotyApp(
                    viewModel = viewModel,
                    backupViewModel = backupViewModel,
                    pendingAddNote = addNoteFromTile,
                    onConsumeAddNote = { addNoteFromTile = false }
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
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
