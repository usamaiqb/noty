package com.noty.app.ui

import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noty.app.data.Note
import com.noty.app.data.NoteType
import java.util.Calendar
import java.util.TimeZone

// ─── Theme ───────────────────────────────────────────────────────────────────

@Composable
fun NotyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFFBFC2FF),
            secondary = Color(0xFF4CDADA),
            surface = Color(0xFF1A1B1E),
            background = Color(0xFF1A1B1E)
        )
        else -> lightColorScheme(
            primary = Color(0xFF4F5CD3),
            secondary = Color(0xFF006A6A),
            surface = Color(0xFFFDFBFF),
            background = Color(0xFFFDFBFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ─── Root composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotyApp(
    viewModel: NotyViewModel,
    triggerAddNote: Boolean = false,
    onAddNoteTriggered: () -> Unit = {}
) {
    val notes by viewModel.allNotes.observeAsState(emptyList())
    var showAddSheet by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val filteredNotes by remember(notes, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) notes
            else notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    val displayedNotes = if (searchActive) filteredNotes else notes
    val defaultPin by viewModel.defaultPinFlow.collectAsState(initial = true)

    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val searchFocusRequester = remember { FocusRequester() }
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    // Open add sheet when triggered from Quick Settings tile
    LaunchedEffect(triggerAddNote) {
        if (triggerAddNote) {
            showSettings = false
            showAddSheet = true
            onAddNoteTriggered()
        }
    }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    BackHandler(enabled = !showSettings && searchActive) {
        searchActive = false
        searchQuery = ""
    }

    AnimatedContent(
        targetState = showSettings,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
        label = "screens"
    ) { settings ->
        if (settings) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { showSettings = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                topBar = {
                    AnimatedContent(
                        targetState = searchActive,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "topBar"
                    ) { active ->
                        if (active) {
                            SearchTopBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClose = {
                                    searchActive = false
                                    searchQuery = ""
                                },
                                focusRequester = searchFocusRequester
                            )
                        } else {
                            LargeTopAppBar(
                                title = { Text("Noty") },
                                actions = {
                                    IconButton(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                        searchActive = true
                                    }) {
                                        Icon(Icons.Rounded.Search, contentDescription = "Search notes")
                                    }
                                    IconButton(onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                        showSettings = true
                                    }) {
                                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                                    }
                                },
                                colors = TopAppBarDefaults.largeTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                scrollBehavior = scrollBehavior
                            )
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text("New Note") },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        expanded = fabExpanded,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            showAddSheet = true
                        }
                    )
                }
            ) { innerPadding ->
                when {
                    displayedNotes.isEmpty() -> EmptyStateContent(
                        modifier = Modifier.padding(innerPadding),
                        searchQuery = if (searchActive) searchQuery else ""
                    )
                    else -> NotesList(
                        notes = displayedNotes,
                        listState = listState,
                        onEditClick = { noteToEdit = it },
                        onPinClick = { viewModel.update(it.copy(isPinned = !it.isPinned)) },
                        onDeleteClick = { noteToDelete = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    // Add note bottom sheet
    if (showAddSheet) {
        NoteBottomSheet(
            defaultPinned = defaultPin,
            onDismiss = { showAddSheet = false },
            onSave = { title, description, isPinned, reminderAt ->
                viewModel.insert(
                    Note(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        type = NoteType.NOTE,
                        isPinned = isPinned,
                        reminderAt = reminderAt
                    )
                )
                showAddSheet = false
            }
        )
    }

    // Edit note bottom sheet
    noteToEdit?.let { note ->
        NoteBottomSheet(
            note = note,
            onDismiss = { noteToEdit = null },
            onSave = { title, description, isPinned, reminderAt ->
                viewModel.update(
                    note.copy(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        isPinned = isPinned,
                        reminderAt = reminderAt
                    )
                )
                noteToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete this note?") },
            text = { Text("Are you sure you want to delete '${note.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(note)
                        noteToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

}

// ─── Search top bar ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester
) {
    val focusManager = LocalFocusManager.current

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close search")
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search notes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

// ─── Notes list ───────────────────────────────────────────────────────────────

@Composable
private fun NotesList(
    notes: List<Note>,
    listState: LazyListState,
    onEditClick: (Note) -> Unit,
    onPinClick: (Note) -> Unit,
    onDeleteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val (pinned, others) = notes.partition { it.isPinned }
    val showHeaders = pinned.isNotEmpty()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = 88.dp
        ),
        modifier = modifier
    ) {
        if (showHeaders) {
            item(key = "pinned_header") {
                Box(modifier = Modifier.animateItem()) {
                    SectionHeader("Pinned")
                }
            }
        }
        itemsIndexed(pinned, key = { _, note -> note.id }) { index, note ->
            NoteCard(
                note = note,
                position = segmentPositionFor(index, pinned.size),
                onEditClick = { onEditClick(note) },
                onPinClick = { onPinClick(note) },
                onDeleteClick = { onDeleteClick(note) },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = tween(durationMillis = 180),
                        fadeOutSpec = tween(durationMillis = 120),
                        placementSpec = tween(durationMillis = 200)
                    )
                    .padding(bottom = 2.dp)
            )
        }
        if (showHeaders && others.isNotEmpty()) {
            item(key = "others_header") {
                Box(modifier = Modifier.animateItem().padding(top = 20.dp)) {
                    SectionHeader("Others")
                }
            }
        }
        itemsIndexed(others, key = { _, note -> note.id }) { index, note ->
            NoteCard(
                note = note,
                position = segmentPositionFor(index, others.size),
                onEditClick = { onEditClick(note) },
                onPinClick = { onPinClick(note) },
                onDeleteClick = { onDeleteClick(note) },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = tween(durationMillis = 180),
                        fadeOutSpec = tween(durationMillis = 120),
                        placementSpec = tween(durationMillis = 200)
                    )
                    .padding(bottom = 2.dp)
            )
        }
    }
}

// ─── Note card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    position: SegmentPosition,
    onEditClick: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val relativeTime = remember(note.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            note.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Surface(
        shape = segmentShape(position),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onEditClick()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(
                            if (note.isPinned) HapticFeedbackType.ToggleOff
                            else HapticFeedbackType.ToggleOn
                        )
                        onPinClick()
                    },
                    onLongClickLabel = if (note.isPinned) "Unpin note" else "Pin note"
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                color = if (note.isPinned) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = note.isPinned,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "noteChipIcon"
                    ) { pinned ->
                        Icon(
                            imageVector = if (pinned) Icons.Rounded.PushPin
                                          else Icons.AutoMirrored.Rounded.Notes,
                            contentDescription = null,
                            tint = if (pinned) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!note.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    note.reminderAt?.let { reminderAt ->
                        val context = LocalContext.current
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = "Reminder set",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatReminderTime(context, reminderAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        menuExpanded = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (note.isPinned) "Unpin" else "Pin") },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        onClick = {
                            haptics.performHapticFeedback(
                                if (note.isPinned) HapticFeedbackType.ToggleOff
                                else HapticFeedbackType.ToggleOn
                            )
                            menuExpanded = false
                            onPinClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            menuExpanded = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

// ─── Add / Edit note bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteBottomSheet(
    note: Note? = null,
    defaultPinned: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, isPinned: Boolean, reminderAt: Long?) -> Unit
) {
    val isEditing = note != null
    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: defaultPinned) }
    var reminderAt by remember { mutableStateOf(note?.reminderAt) }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

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
                    text = if (isEditing) "Edit Note" else "New Note",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pin note", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { checked ->
                            haptics.performHapticFeedback(
                                if (checked) HapticFeedbackType.ToggleOn
                                else HapticFeedbackType.ToggleOff
                            )
                            isPinned = checked
                        },
                        thumbContent = {
                            AnimatedContent(
                                targetState = isPinned,
                                transitionSpec = {
                                    fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                                },
                                label = "pinThumbIcon"
                            ) { checked ->
                                Icon(
                                    imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
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
                label = { Text("Note Title") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reminder picker pill
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showDatePicker = true
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
                        text = reminderAt?.let { formatReminderTime(context, it) } ?: "Add reminder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (reminderAt != null) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (reminderAt != null) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                                reminderAt = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove reminder",
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
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
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
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSave(trimmed, description.trim(), isPinned, reminderAt)
                        } else {
                            haptics.performHapticFeedback(HapticFeedbackType.Reject)
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
                    Text(if (isEditing) "Update" else "Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderAt ?: System.currentTimeMillis()
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
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
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
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        // DatePicker returns UTC midnight; combine with the picked
                        // time in the local timezone
                        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = pendingDateMillis ?: System.currentTimeMillis()
                        }
                        reminderAt = Calendar.getInstance().apply {
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
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatReminderTime(context: android.content.Context, millis: Long): String =
    DateUtils.formatDateTime(
        context, millis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH
    )

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
fun EmptyStateContent(modifier: Modifier = Modifier, searchQuery: String = "") {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (searchQuery.isBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Notes,
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No notes yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "Tap + New Note to create your first note",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No results for \"$searchQuery\"",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = "Try a different search term",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1.3f))
    }
}
