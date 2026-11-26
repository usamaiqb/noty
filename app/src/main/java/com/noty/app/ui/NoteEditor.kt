package com.noty.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.RadioButton
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
import com.noty.app.data.withPinWinning
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
 *
 * An edit is compared with the note as the pinned-or-scheduled rule reads it, since that is
 * what the editor shows: a pinned note saved with a time from before the rule opens without it.
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
    val shown = original.withPinWinning()
    title.trim() != shown.title ||
        description.trim() != shown.description.orEmpty() ||
        isPinned != shown.isPinned ||
        reminderAt != shown.reminderAt
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
    // Pinned or scheduled, never both. Pin on clears the time; picking a time turns Pin off.
    var reminderAt by remember { mutableStateOf(note?.withPinWinning()?.reminderAt) }
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
    val openSchedulePicker = {
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
                // Pin is a one-tap toggle; the clock schedules a pin for later. Once a time is set
                // the clock alone stands for the choice, so nothing can drop that time quietly.
                PinControls(
                    isPinned = isPinned,
                    scheduledAt = reminderAt,
                    canPin = canPin,
                    canSchedule = canPin && canScheduleReminders,
                    onDontPin = {
                        haptics.toggle(on = false)
                        isPinned = false
                        reminderAt = null
                    },
                    onPinNow = {
                        if (pinBlocked) {
                            haptics.reject()
                            onPinBlocked()
                        } else {
                            haptics.toggle(on = true)
                            isPinned = true
                            reminderAt = null
                        }
                    },
                    onPinLater = openSchedulePicker
                )
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
            // A scheduled pin shows as a chip: tap to change the time, ✕ to remove it.
            reminderAt?.let { at ->
                InputChip(
                    selected = true,
                    onClick = openSchedulePicker,
                    label = { Text(formatReminderTime(context, at)) },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = stringResource(R.string.cd_scheduled),
                            modifier = Modifier.size(InputChipDefaults.IconSize)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_remove_schedule),
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
            icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
            title = { Text(stringResource(R.string.reminder_permission_title)) },
            text = {
                Text(stringResource(R.string.schedule_permission_message))
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
                            // It gets pinned at that time, so it isn't pinned until then.
                            isPinned = false
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

private enum class PinState { OFF, NOW, LATER }

private class PinOption(
    val state: PinState,
    val text: String,
    val available: Boolean,
    val choose: () -> Unit
)

/** Material's standard disabled-content opacity. */
private const val DisabledAlpha = 0.38f

/**
 * The editor's pin controls.
 *
 * Not scheduled: a Pin toggle (one tap, the common case) and a clock that opens the date and
 * time pickers. Scheduled: the Pin toggle steps aside and the clock opens a single-select menu
 * (Don't pin, Pin now, Pin at the time), so the picked time is only given up by a labelled
 * choice, never as the side effect of a tap. Clearing the schedule brings the toggle back.
 *
 * A control the app can't honour reads as disabled: pinning without notifications, scheduling
 * without notifications or exact alarms. Like every blocked pin control in the app it stays
 * tappable, so a tap explains what's missing and how to turn it on.
 */
@Composable
private fun PinControls(
    isPinned: Boolean,
    scheduledAt: Long?,
    canPin: Boolean,
    canSchedule: Boolean,
    onDontPin: () -> Unit,
    onPinNow: () -> Unit,
    onPinLater: () -> Unit
) {
    if (scheduledAt != null) {
        ScheduledPinMenu(scheduledAt, canPin, onDontPin, onPinNow, onChangeTime = onPinLater)
        return
    }
    val pinLabel = stringResource(if (isPinned) R.string.cd_unpin_note else R.string.cd_pin_note)
    WithTooltip(pinLabel) {
        // The state is carried by the icon: filled when pinned, outlined when not.
        IconToggleButton(
            checked = isPinned,
            onCheckedChange = { pinned -> if (pinned) onPinNow() else onDontPin() },
            modifier = Modifier.alpha(if (!isPinned && !canPin) DisabledAlpha else 1f)
        ) {
            Icon(if (isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin, contentDescription = pinLabel)
        }
    }
    val scheduleLabel = stringResource(R.string.pin_choice_later)
    WithTooltip(scheduleLabel) {
        IconButton(
            onClick = onPinLater,
            modifier = Modifier.alpha(if (canSchedule) 1f else DisabledAlpha)
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = scheduleLabel)
        }
    }
}

/** The clock on a scheduled note: when the note gets pinned, as a single-select menu. */
@Composable
private fun ScheduledPinMenu(
    scheduledAt: Long,
    canPin: Boolean,
    onDontPin: () -> Unit,
    onPinNow: () -> Unit,
    onChangeTime: () -> Unit
) {
    val context = LocalContext.current
    val pinAt = stringResource(R.string.pin_choice_later_at, formatReminderTime(context, scheduledAt))
    val options = listOf(
        PinOption(PinState.OFF, stringResource(R.string.pin_choice_off), available = true, onDontPin),
        PinOption(PinState.NOW, stringResource(R.string.pin_choice_now), available = canPin, onPinNow),
        // The current choice; picking it again changes the time.
        PinOption(PinState.LATER, pinAt, available = true, onChangeTime)
    )
    val label = stringResource(R.string.cd_pin_options)
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        WithTooltip(label) {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.semantics { stateDescription = pinAt }
            ) {
                Icon(Icons.Rounded.Schedule, contentDescription = label)
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            options.forEach { option ->
                PinOptionItem(option, selected = option.state == PinState.LATER) {
                    menuExpanded = false
                    option.choose()
                }
            }
        }
    }
}

@Composable
private fun PinOptionItem(option: PinOption, selected: Boolean, onClick: () -> Unit) {
    // The current choice never dims: it's what the note is, even if it couldn't be chosen
    // again right now.
    val dimmed = !option.available && !selected
    DropdownMenuItem(
        text = { Text(option.text) },
        // A radio marks the one current choice, the standard single-select menu.
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
        modifier = Modifier
            .alpha(if (dimmed) DisabledAlpha else 1f)
            .semantics { this.selected = selected }
    )
}
