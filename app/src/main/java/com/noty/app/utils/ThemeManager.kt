package com.noty.app.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeManager(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        val DEFAULT_PIN_KEY = booleanPreferencesKey("default_pin_new_notes")
        val SHOW_UNPIN_OPTION_KEY = booleanPreferencesKey("show_unpin_option")
        val NOTE_DESCRIPTION_LINES_KEY = intPreferencesKey("note_description_lines")
    }

    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }

    val themeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(themeName)
    }

    val dynamicColorsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS_KEY] ?: true
    }

    val defaultPinFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_PIN_KEY] ?: false
    }

    val showUnpinOptionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_UNPIN_OPTION_KEY] ?: false
    }

    val noteDescriptionLinesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[NOTE_DESCRIPTION_LINES_KEY] ?: 2
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_KEY] = enabled
        }
    }

    suspend fun setDefaultPin(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_PIN_KEY] = enabled
        }
    }

    suspend fun setShowUnpinOption(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_UNPIN_OPTION_KEY] = enabled
        }
    }

    suspend fun setDescriptionLines(lines: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTE_DESCRIPTION_LINES_KEY] = lines
        }
    }
}
