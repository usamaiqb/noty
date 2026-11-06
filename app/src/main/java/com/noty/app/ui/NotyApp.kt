package com.noty.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.noty.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noty.app.data.Note
import com.noty.app.ui.expressive.CollapsingLargeTopBar
import com.noty.app.ui.expressive.rememberExpressiveHaptics
import com.noty.app.ui.expressive.rememberExpressiveScrollBehavior
import com.noty.app.data.NoteType
// ─── Root composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotyApp(
    viewModel: NotyViewModel,
    triggerAddNote: Boolean = false,
    onAddNoteTriggered: () -> Unit = {}
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle(emptyList())
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
    val defaultPin by viewModel.defaultPinFlow.collectAsStateWithLifecycle(true)

    val haptics = rememberExpressiveHaptics()
    val listState = rememberLazyListState()
    val scrollBehavior = rememberExpressiveScrollBehavior()
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
                            CollapsingLargeTopBar(
                                title = stringResource(R.string.app_name),
                                scrollBehavior = scrollBehavior,
                                actions = {
                                    IconButton(onClick = {
                                        haptics.confirm()
                                        searchActive = true
                                    }) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = stringResource(R.string.cd_search_notes)
                                        )
                                    }
                                    IconButton(onClick = {
                                        haptics.confirm()
                                        showSettings = true
                                    }) {
                                        Icon(
                                            Icons.Rounded.Settings,
                                            contentDescription = stringResource(R.string.cd_settings)
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text(stringResource(R.string.home_new_note)) },
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        expanded = fabExpanded,
                        onClick = {
                            haptics.confirm()
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
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message, note.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(note)
                        noteToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
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
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_close_search)
                )
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
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
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear_search))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}
