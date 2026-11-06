package com.noty.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.expressive.GroupGap
import com.noty.app.ui.expressive.SectionGap
import com.noty.app.ui.expressive.SectionHeader
import com.noty.app.ui.expressive.segmentPositionFor
// ─── Notes list ───────────────────────────────────────────────────────────────

@Composable
internal fun NotesList(
    notes: List<Note>,
    listState: LazyListState,
    canPin: Boolean,
    onEditClick: (Note) -> Unit,
    onPinClick: (Note) -> Unit,
    onDeleteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val (pinned, others) = notes.partition { it.isPinned }
    // The "Others" header only earns its place once there is a "Pinned" one to contrast it
    // with; on a list with nothing pinned it would be labelling the whole screen.
    val headed = pinned.isNotEmpty()
    // Resolved here because LazyColumn's content lambda is a LazyListScope, not a
    // composable one, so stringResource cannot be called from inside it.
    val pinnedHeader = if (headed) stringResource(R.string.home_section_pinned) else null
    val othersHeader = if (headed) stringResource(R.string.home_section_others) else null

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = 8.dp, bottom = 88.dp
        ),
        modifier = modifier
    ) {
        val actions = NoteRowActions(canPin, onEditClick, onPinClick, onDeleteClick)
        noteSection(
            headerKey = "pinned_header",
            notes = pinned,
            actions = actions,
            header = pinnedHeader
        )
        noteSection(
            headerKey = "others_header",
            notes = others,
            actions = actions,
            header = othersHeader,
            topPadding = SectionGap
        )
    }
}

/** What every row in the list can do, passed to each section as one unit. */
private class NoteRowActions(
    val canPin: Boolean,
    val onEditClick: (Note) -> Unit,
    val onPinClick: (Note) -> Unit,
    val onDeleteClick: (Note) -> Unit
)

/**
 * One connected group of notes: an optional header, then the rows, each shaped for its place
 * in the group.
 *
 * A LazyListScope extension rather than a composable because every note has to stay its own
 * lazy item — wrapping the group in one item would give `animateItem()` a single block to
 * move instead of the individual rows it is there to track.
 */
private fun LazyListScope.noteSection(
    headerKey: String,
    notes: List<Note>,
    actions: NoteRowActions,
    header: String? = null,
    topPadding: Dp = 0.dp
) {
    if (notes.isEmpty()) return

    if (header != null) {
        item(key = headerKey) {
            Box(modifier = Modifier.animateItem().padding(top = topPadding)) {
                SectionHeader(header)
            }
        }
    }
    itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
        NoteCard(
            note = note,
            position = segmentPositionFor(index, notes.size),
            canPin = actions.canPin,
            onEditClick = { actions.onEditClick(note) },
            onPinClick = { actions.onPinClick(note) },
            onDeleteClick = { actions.onDeleteClick(note) },
            modifier = Modifier
                .animateItem(
                    fadeInSpec = tween(durationMillis = 180),
                    fadeOutSpec = tween(durationMillis = 120),
                    placementSpec = tween(durationMillis = 200)
                )
                .padding(bottom = GroupGap)
        )
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
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.home_empty_subtitle),
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
                text = stringResource(R.string.search_empty_title, searchQuery),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                text = stringResource(R.string.search_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1.3f))
    }
}
