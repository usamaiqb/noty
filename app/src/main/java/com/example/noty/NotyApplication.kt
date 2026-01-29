package com.example.noty

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.noty.utils.ThemeManager
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        
        val themeManager = ThemeManager(this)
        CoroutineScope(Dispatchers.Main).launch {
            themeManager.themeFlow.collect { theme ->
                applyTheme(theme)
            }
        }
    }
    
    private fun applyTheme(themeMode: ThemeManager.ThemeMode) {
        val mode = when (themeMode) {
            ThemeManager.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeManager.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeManager.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
