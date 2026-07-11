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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noty.app.data.Note
import com.noty.app.data.NoteType
import com.noty.app.utils.ThemeManager

// ─── Theme ───────────────────────────────────────────────────────────────────

@Composable
fun NotyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
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
    var showThemeSheet by remember { mutableStateOf(false) }
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
            showAddSheet = true
            onAddNoteTriggered()
        }
    }

    BackHandler(enabled = searchActive) {
        searchActive = false
        searchQuery = ""
    }

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
                                showThemeSheet = true
                            }) {
                                Icon(Icons.Rounded.Palette, contentDescription = "Change theme")
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
                onDeleteClick = { noteToDelete = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    // Add note bottom sheet
    if (showAddSheet) {
        NoteBottomSheet(
            onDismiss = { showAddSheet = false },
            onSave = { title, description, isPinned ->
                viewModel.insert(
                    Note(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        type = NoteType.NOTE,
                        isPinned = isPinned
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
            onSave = { title, description, isPinned ->
                viewModel.update(
                    note.copy(
                        title = title,
                        description = if (description.isEmpty()) null else description,
                        isPinned = isPinned
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

    // Theme selection bottom sheet
    if (showThemeSheet) {
        ThemeSelectionSheet(
            viewModel = viewModel,
            onDismiss = { showThemeSheet = false }
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
    onDeleteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = 88.dp
        ),
        modifier = modifier
    ) {
        itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
            NoteCard(
                note = note,
                position = segmentPositionFor(index, notes.size),
                onEditClick = { onEditClick(note) },
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuExpanded = true
                    },
                    onLongClickLabel = "Note options"
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, isPinned: Boolean) -> Unit
) {
    val isEditing = note != null
    var title by remember { mutableStateOf(note?.title ?: "") }
    var description by remember { mutableStateOf(note?.description ?: "") }
    var isPinned by remember { mutableStateOf(note?.isPinned ?: true) }
    var titleError by remember { mutableStateOf(false) }
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
                            onSave(trimmed, description.trim(), isPinned)
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
}

// ─── Theme selection bottom sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    viewModel: NotyViewModel,
    onDismiss: () -> Unit
) {
    val currentTheme by viewModel.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption(
                label = "System",
                description = "Follow device setting",
                selected = currentTheme == ThemeManager.ThemeMode.SYSTEM,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.setTheme(ThemeManager.ThemeMode.SYSTEM)
                    onDismiss()
                }
            )
            ThemeOption(
                label = "Light",
                description = "Always use light theme",
                selected = currentTheme == ThemeManager.ThemeMode.LIGHT,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.setTheme(ThemeManager.ThemeMode.LIGHT)
                    onDismiss()
                }
            )
            ThemeOption(
                label = "Dark",
                description = "Always use dark theme",
                selected = currentTheme == ThemeManager.ThemeMode.DARK,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    viewModel.setTheme(ThemeManager.ThemeMode.DARK)
                    onDismiss()
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else null,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

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
