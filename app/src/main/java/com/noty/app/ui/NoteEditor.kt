package com.noty.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.expressive.rememberExpressiveHaptics
import com.noty.app.utils.ReminderScheduler
import java.util.Calendar
import java.util.TimeZone

// ─── Add / Edit note: full-screen editor ──────────────────────────────────────

/**
 * Whether closing the editor now would lose something the user did.
 *
 * Text is compared trimmed because saving trims it: a stray space changes nothing that would
 * be saved, so it shouldn't stop the editor closing. A new note counts only what was entered,
 * not the pin toggle: it starts from a setting, and asking "Discard?" over an empty note
 * whose only change is that toggle would be noise.
 */
internal fun hasUnsavedChanges(
    original: Note?,
    title: String,
    description: String,
    isPinned: Boolean,
    reminderAt: Long?
): Boolean = if (original == null) {
    title.isNotBlank() || description.isNotBlank() || reminderAt != null
} else {
    title.trim() != original.title ||
        description.trim() != original.description.orEmpty() ||
        isPinned != original.isPinned ||
        reminderAt != original.reminderAt
}

/**
 * Full-screen add/edit editor. ✕ and back both close it; with unsaved changes they ask first
 * (#69). [onDelete] is null for a new note, which hides the delete action.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteEditor(
    note: Note? = null,
    defaultPinned: Boolean = true,
    canPin: Boolean = true,
    onPinBlocked: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
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

    val hasChanges by remember {
        derivedStateOf { hasUnsavedChanges(note, title, description, isPinned, reminderAt) }
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestDismiss = {
        if (hasChanges) showDiscardDialog = true else onDismiss()
    }

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

    // No boxes around the fields: in a full-screen editor the page is the field.
    val borderless = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    )
    val titleFocus = remember { FocusRequester() }

    BackHandler(onBack = requestDismiss)

    // A reminder is a notification, so it needs notifications first (same explanation as
    // the pin), then exact alarms. Without either it would fire unseen.
    val openReminderPicker = {
        when {
            !canPin -> {
                haptics.reject()
                onPinBlocked()
            }
            !canScheduleReminders -> {
                haptics.open()
                showReminderPermissionDialog = true
            }
            else -> {
                haptics.open()
                showDatePicker = true
            }
        }
    }

    // Opening a note just to read it is normal, so ✓ on an unchanged note means "done":
    // close without writing to the database or re-posting its notification.
    val save = {
        val trimmed = title.trim()
        if (isEditing && !hasChanges) {
            onDismiss()
        } else if (trimmed.isNotEmpty()) {
            haptics.confirm()
            onSave(trimmed, description.trim(), isPinned, reminderAt)
        } else {
            haptics.reject()
            titleError = true
        }
    }

    Scaffold(
        // The floating toolbar sits in the FAB slot, so including the keyboard here keeps it
        // above the keyboard and shrinks the content to the space that's left.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.sheet_title_edit else R.string.sheet_title_new))
                },
                navigationIcon = {
                    IconButton(onClick = requestDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_editor))
                    }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Expressive floating toolbar: the note's own actions, with Save as its FAB.
            HorizontalFloatingToolbar(
                expanded = true,
                floatingActionButton = {
                    val saveLabel = stringResource(if (isEditing) R.string.action_update else R.string.action_save)
                    WithTooltip(saveLabel) {
                        FloatingToolbarDefaults.StandardFloatingActionButton(onClick = save) {
                            Icon(Icons.Rounded.Check, contentDescription = saveLabel)
                        }
                    }
                }
            ) {
                val pinLabel = stringResource(if (isPinned) R.string.cd_unpin_note else R.string.cd_pin_note)
                WithTooltip(pinLabel) {
                    // The state is carried by the icon: filled when pinned, outlined when not.
                    IconToggleButton(
                        checked = isPinned,
                        onCheckedChange = { checked ->
                            if (pinBlocked) {
                                haptics.reject()
                                onPinBlocked()
                            } else {
                                haptics.toggle(on = checked)
                                isPinned = checked
                            }
                        }
                    ) {
                        Icon(
                            if (isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                            contentDescription = pinLabel
                        )
                    }
                }
                val reminderLabel = stringResource(R.string.reminder_add)
                WithTooltip(reminderLabel) {
                    IconButton(onClick = openReminderPicker) {
                        Icon(Icons.Rounded.Alarm, contentDescription = reminderLabel)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp)
        ) {
            TextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                placeholder = {
                    Text(stringResource(R.string.sheet_label_title), style = MaterialTheme.typography.titleLarge)
                },
                textStyle = MaterialTheme.typography.titleLarge,
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.sheet_error_title_required)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                colors = borderless,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
            )
            // A set reminder shows as a chip: tap to change it, ✕ to remove it.
            reminderAt?.let { at ->
                InputChip(
                    selected = true,
                    onClick = openReminderPicker,
                    label = { Text(formatReminderTime(context, at)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(InputChipDefaults.IconSize)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_remove_reminder),
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                .clickable(role = Role.Button) {
                                    haptics.toggle(on = false)
                                    reminderAt = null
                                }
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            // Fills the rest of the screen and scrolls itself, so a long description has
            // room (#80) and a tap anywhere below the title starts typing.
            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text(stringResource(R.string.sheet_label_description)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = borderless,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    // A new note is for typing; an existing one opens for reading.
    LaunchedEffect(Unit) {
        if (!isEditing) titleFocus.requestFocus()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    stringResource(
                        if (isEditing) R.string.discard_dialog_title_edit
                        else R.string.discard_dialog_title_new
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        showDiscardDialog = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.action_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            }
        )
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

/** Long-press label for an icon-only button, as Material asks of every icon button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithTooltip(text: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        content = content
    )
}
