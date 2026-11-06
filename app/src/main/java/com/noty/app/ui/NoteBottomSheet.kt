package com.noty.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.expressive.ExpressiveSwitch
import com.noty.app.ui.expressive.rememberExpressiveHaptics
import com.noty.app.utils.ReminderScheduler
import java.util.Calendar
import java.util.TimeZone
// ─── Add / Edit note bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    note: Note? = null,
    defaultPinned: Boolean = true,
    canPin: Boolean = true,
    onPinBlocked: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, isPinned: Boolean, reminderAt: Long?) -> Unit
) {
    val isEditing = note != null
    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    // "New notes start pinned" can't apply while notifications are off. An existing pinned
    // note keeps its state, and can still be unpinned.
    var isPinned by remember { mutableStateOf(note?.isPinned ?: (defaultPinned && canPin)) }
    val pinBlocked = !isPinned && !canPin
    var reminderAt by remember { mutableStateOf(note?.reminderAt) }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val haptics = rememberExpressiveHaptics()

    // Reminders are gated on the exact-alarm permission: without it Android can
    // delay delivery by 10+ minutes, so the feature is hidden rather than degraded.
    var canScheduleReminders by remember { mutableStateOf(ReminderScheduler.canScheduleExact(context)) }
    var showReminderPermissionDialog by remember { mutableStateOf(false) }
    var timeInPastError by remember { mutableStateOf(false) }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        canScheduleReminders = ReminderScheduler.canScheduleExact(context)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header: title + sticky toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (isEditing) R.string.sheet_title_edit else R.string.sheet_title_new),
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // While pinning is blocked the switch is drawn disabled, which also stops it
                    // taking taps, so the row takes them instead and explains why.
                    modifier = if (pinBlocked) {
                        Modifier.clickable(role = Role.Switch) {
                            haptics.reject()
                            onPinBlocked()
                        }
                    } else {
                        Modifier
                    }
                ) {
                    Text(
                        stringResource(R.string.sheet_pin_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (pinBlocked) 0.38f else 1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    ExpressiveSwitch(
                        checked = isPinned,
                        onCheckedChange = if (pinBlocked) {
                            null
                        } else {
                            { checked ->
                                haptics.toggle(on = checked)
                                isPinned = checked
                            }
                        },
                        enabled = !pinBlocked
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text(stringResource(R.string.sheet_label_title)) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.sheet_error_title_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.sheet_label_description)) },
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reminder picker pill
            Surface(
                onClick = {
                    haptics.open()
                    if (canScheduleReminders) {
                        showDatePicker = true
                    } else {
                        showReminderPermissionDialog = true
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = if (reminderAt != null) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = if (reminderAt != null) MaterialTheme.colorScheme.onSecondaryContainer
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = reminderAt?.let { formatReminderTime(context, it) }
                            ?: stringResource(
                                if (canScheduleReminders) R.string.reminder_add
                                else R.string.reminder_add_needs_permission
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (reminderAt != null) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (reminderAt != null) {
                        IconButton(
                            onClick = {
                                haptics.toggle(on = false)
                                reminderAt = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_remove_reminder),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Paired action buttons: mirrored asymmetric corners read as one unit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        haptics.confirm()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(
                        topStart = 28.dp, topEnd = 12.dp,
                        bottomStart = 28.dp, bottomEnd = 12.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isNotEmpty()) {
                            haptics.confirm()
                            onSave(trimmed, description.trim(), isPinned, reminderAt)
                        } else {
                            haptics.reject()
                            titleError = true
                        }
                    },
                    shape = RoundedCornerShape(
                        topStart = 12.dp, topEnd = 28.dp,
                        bottomStart = 12.dp, bottomEnd = 28.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Text(stringResource(if (isEditing) R.string.action_update else R.string.action_save))
                }
            }
        }
    }

    if (showReminderPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showReminderPermissionDialog = false },
            icon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
            title = { Text(stringResource(R.string.reminder_permission_title)) },
            text = {
                Text(stringResource(R.string.reminder_permission_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReminderPermissionDialog = false
                        exactAlarmSettingsLauncher.launch(
                            ReminderScheduler.exactAlarmSettingsIntent(context)
                        )
                    }
                ) {
                    Text(stringResource(R.string.action_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderPermissionDialog = false }) {
                    Text(stringResource(R.string.action_not_now))
                }
            }
        )
    }

    if (showDatePicker) {
        // DatePicker works in UTC, so "today" has to be expressed as UTC midnight
        // of the current *local* date or the boundary lands a day off.
        val todayUtcMidnight = remember {
            val local = Calendar.getInstance()
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    local.get(Calendar.YEAR),
                    local.get(Calendar.MONTH),
                    local.get(Calendar.DAY_OF_MONTH)
                )
            }.timeInMillis
        }
        val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
        val datePickerState = rememberDatePickerState(
            // A stored reminder can be in the past only if it never fired; don't
            // seed the picker with a date it would reject.
            initialSelectedDateMillis = (reminderAt ?: System.currentTimeMillis())
                .coerceAtLeast(System.currentTimeMillis()),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis >= todayUtcMidnight

                override fun isSelectableYear(year: Int) = year >= currentYear
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                        if (pendingDateMillis != null) showTimePicker = true
                    }
                ) {
                    Text(stringResource(R.string.action_next))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialCalendar = remember {
            Calendar.getInstance().apply { reminderAt?.let { timeInMillis = it } }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCalendar.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                timeInPastError = false
            },
            title = { Text(stringResource(R.string.reminder_time_dialog_title)) },
            text = {
                Column {
                    TimePicker(state = timePickerState)
                    if (timeInPastError) {
                        Text(
                            text = stringResource(R.string.reminder_time_in_past),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // DatePicker returns UTC midnight; combine with the picked
                        // time in the local timezone
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = pendingDateMillis ?: System.currentTimeMillis()
                        }
                        val candidate = Calendar.getInstance().apply {
                            set(
                                utc.get(Calendar.YEAR),
                                utc.get(Calendar.MONTH),
                                utc.get(Calendar.DAY_OF_MONTH),
                                timePickerState.hour,
                                timePickerState.minute,
                                0
                            )
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        // The date picker blocks past days, but not an earlier
                        // time on today — an alarm set in the past fires instantly.
                        if (candidate <= System.currentTimeMillis()) {
                            haptics.reject()
                            timeInPastError = true
                        } else {
                            haptics.confirm()
                            reminderAt = candidate
                            timeInPastError = false
                            showTimePicker = false
                        }
                    }
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        timeInPastError = false
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
