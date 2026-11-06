package com.noty.app.ui

import android.app.Application
import android.content.Intent
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.noty.app.R
import com.noty.app.data.AppDatabase
import com.noty.app.data.NotyRepository
import com.noty.app.data.Note
import com.noty.app.utils.NotificationHelper
import com.noty.app.utils.ReminderScheduler
import com.noty.app.utils.ThemeManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** A short result shown as a snackbar, because the screen that asked for it has moved on. */
sealed interface UserMessage {
    fun text(resources: Resources): String
}

/** A note change that didn't happen, or only partly did. */
enum class NoteMessage(@StringRes private val res: Int) : UserMessage {
    SAVE_FAILED(R.string.message_save_failed),
    UPDATE_FAILED(R.string.message_update_failed),
    DELETE_FAILED(R.string.message_delete_failed),
    REMINDER_NEEDS_PERMISSION(R.string.message_reminder_needs_permission),
    REMINDER_TIME_PASSED(R.string.message_reminder_time_passed);

    override fun text(resources: Resources): String = resources.getString(res)
}

class NotyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotyRepository
    private val themeManager: ThemeManager
    private val notificationHelper: NotificationHelper

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NotyRepository(noteDao)
        themeManager = ThemeManager(application)
        notificationHelper = NotificationHelper(application)
    }

    val allNotes = repository.getAllNotes()
    val themeFlow = themeManager.themeFlow
    val dynamicColorsFlow = themeManager.dynamicColorsFlow
    val defaultPinFlow = themeManager.defaultPinFlow

    // The sheet closes as soon as Save is tapped, so a failure has to be reported
    // after the fact rather than inline.
    private val _messages = Channel<UserMessage>(Channel.BUFFERED)
    val messages: Flow<UserMessage> = _messages.receiveAsFlow()

    // Track service state to prevent start/stop thrashing
    private var isServiceRunning = false

    init {
        // Restore all notifications on app start
        syncNotifications()

        // Manage background service lifecycle based on note existence
        // android:stopWithTask="false" in manifest ensures this stays alive after swipe
        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                val shouldRun = notes.any { it.isPinned }
                if (shouldRun && !isServiceRunning) {
                    val intent = Intent(application, com.noty.app.utils.NotyService::class.java)
                    ContextCompat.startForegroundService(application, intent)
                    isServiceRunning = true
                } else if (!shouldRun && isServiceRunning) {
                    val intent = Intent(application, com.noty.app.utils.NotyService::class.java)
                    application.stopService(intent)
                    isServiceRunning = false
                }
            }
        }
    }


    /** Posts any pinned note whose notification is missing, e.g. after notifications are turned back on. */
    fun syncNotifications() = viewModelScope.launch {
        notificationHelper.syncNotifications(repository.getAllNotes().first())
    }

    fun insert(note: Note) = viewModelScope.launch {
        val id = repository.insert(note)
        if (id <= 0) {
            _messages.send(NoteMessage.SAVE_FAILED)
            return@launch
        }
        val noteWithId = note.copy(id = id)
        if (noteWithId.isPinned) {
            notificationHelper.showNotification(noteWithId)
        }
        // If the exact-alarm permission went away between picking the time
        // and saving, drop the reminder rather than store one that can't fire.
        if (noteWithId.reminderAt != null &&
            !ReminderScheduler.schedule(getApplication(), noteWithId)
        ) {
            repository.clearReminder(id)
            _messages.send(reminderNotSetReason())
        }
    }

    fun update(note: Note) = viewModelScope.launch {
        // Leave notifications and alarms alone if the change never reached the database,
        // so the shade keeps matching what is stored.
        if (!repository.update(note)) {
            _messages.send(NoteMessage.UPDATE_FAILED)
            return@launch
        }
        if (note.isPinned) {
            notificationHelper.showNotification(note)
        } else {
            notificationHelper.cancelNotification(note.id.toInt())
        }
        if (note.reminderAt != null) {
            if (!ReminderScheduler.schedule(getApplication(), note)) {
                repository.clearReminder(note.id)
                _messages.send(reminderNotSetReason())
            }
        } else {
            ReminderScheduler.cancel(getApplication(), note.id)
        }
    }

    /** Pins or unpins from the list. Not an edit, so the note keeps its place. */
    fun setPinned(note: Note, pinned: Boolean) = viewModelScope.launch {
        if (!repository.setPinned(note.id, pinned)) {
            _messages.send(NoteMessage.UPDATE_FAILED)
            return@launch
        }
        if (pinned) {
            notificationHelper.showNotification(note.copy(isPinned = true))
        } else {
            notificationHelper.cancelNotification(note.id.toInt())
        }
    }

    fun delete(note: Note) = viewModelScope.launch {
        if (!repository.delete(note)) {
            _messages.send(NoteMessage.DELETE_FAILED)
            return@launch
        }
        notificationHelper.cancelNotification(note.id.toInt())
        ReminderScheduler.cancel(getApplication(), note.id)
        notificationHelper.cancelReminderNotification(note.id.toInt())
    }

    private fun reminderNotSetReason(): NoteMessage =
        if (ReminderScheduler.canScheduleExact(getApplication())) {
            NoteMessage.REMINDER_TIME_PASSED
        } else {
            NoteMessage.REMINDER_NEEDS_PERMISSION
        }

    fun setTheme(mode: ThemeManager.ThemeMode) = viewModelScope.launch {
        themeManager.setTheme(mode)
    }

    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch {
        themeManager.setDynamicColors(enabled)
    }

    fun setDefaultPin(enabled: Boolean) = viewModelScope.launch {
        themeManager.setDefaultPin(enabled)
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
