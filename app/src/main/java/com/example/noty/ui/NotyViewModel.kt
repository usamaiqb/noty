package com.example.noty.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.noty.data.AppDatabase
import com.example.noty.data.NotyRepository
import com.example.noty.data.Note
import com.example.noty.utils.NotificationHelper
import com.example.noty.utils.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotyRepository
    private val themeManager: ThemeManager
    private val notificationHelper: NotificationHelper

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NotyRepository(noteDao)
        themeManager = ThemeManager(application)
        notificationHelper = NotificationHelper(application)

        // Restore all notifications on app start
        viewModelScope.launch {
            val notes = repository.getAllNotes().first()
            notificationHelper.syncNotifications(notes)
        }
    }

    val allNotes = repository.getAllNotes().asLiveData()

    val themeFlow = themeManager.themeFlow

    // No longer need to manage a background service


    fun insert(note: Note) = viewModelScope.launch {
        val id = repository.insert(note)
        if (id > 0) {
            val noteWithId = note.copy(id = id)
            notificationHelper.showNotification(noteWithId)
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        repository.delete(note)
        notificationHelper.cancelNotification(note.id.toInt())
    }

    fun setTheme(mode: ThemeManager.ThemeMode) = viewModelScope.launch {
        themeManager.setTheme(mode)
    }
}

class NotyViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotyViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
