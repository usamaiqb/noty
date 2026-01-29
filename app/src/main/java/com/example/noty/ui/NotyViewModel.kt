package com.example.noty.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.noty.data.AppDatabase
import com.example.noty.data.NotyRepository
import com.example.noty.data.Task
import com.example.noty.utils.NotificationHelper
import com.example.noty.utils.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotyRepository
    private val themeManager: ThemeManager
    private val notificationHelper: NotificationHelper

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = NotyRepository(taskDao)
        themeManager = ThemeManager(application)
        notificationHelper = NotificationHelper(application)
    }

    val allTasks = repository.allTasks.asLiveData()
    
    val themeFlow = themeManager.themeFlow

    // Monitor tasks to manage service
    init {
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                if (tasks.isNotEmpty()) {
                    val intent = android.content.Intent(application, com.example.noty.utils.StickyService::class.java)
                    application.startService(intent)
                } else {
                    val intent = android.content.Intent(application, com.example.noty.utils.StickyService::class.java)
                    application.stopService(intent)
                }
            }
        }
    }

    fun insert(task: Task) = viewModelScope.launch {
        val id = repository.insert(task)
        val taskWithId = task.copy(id = id)
        notificationHelper.showNotification(taskWithId)
    }

    fun delete(task: Task) = viewModelScope.launch {
        repository.delete(task)
        notificationHelper.cancelNotification(task.id.toInt())
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
