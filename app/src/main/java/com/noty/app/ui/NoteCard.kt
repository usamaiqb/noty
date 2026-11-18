package com.noty.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.text.format.DateUtils
import com.noty.app.R
import com.noty.app.data.Note
import com.noty.app.ui.expressive.SegmentPosition
import com.noty.app.ui.expressive.rememberExpressiveHaptics
import com.noty.app.ui.expressive.segmentShape
/** Material's standard disabled-content opacity. */
private const val DisabledAlpha = 0.38f

// ─── Note card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteCard(
    note: Note,
    position: SegmentPosition,
    canPin: Boolean,
    onEditClick: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    // Unpinning always works; pinning needs notifications. A blocked Pin stays tappable so
    // it can explain itself, but reads as disabled.
    val pinBlocked = !note.isPinned && !canPin
    val pinAlpha = if (pinBlocked) DisabledAlpha else 1f
    val haptics = rememberExpressiveHaptics()
    val relativeTime = remember(note.updatedAt) {
        DateUtils.getRelativeTimeSpanString(
            note.updatedAt,
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
                        haptics.confirm()
                        onEditClick()
                    },
                    // Long-press opens the note's menu, as on most Android lists. It used to
                    // toggle the pin, which a long press while scrolling could do by accident.
                    onLongClick = {
                        haptics.longPress()
                        menuExpanded = true
                    },
                    onLongClickLabel = stringResource(R.string.cd_more_options)
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
                            contentDescription = stringResource(R.string.cd_reminder_set),
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
                        haptics.open()
                        menuExpanded = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenuPopup(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 2)) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(if (note.isPinned) R.string.action_unpin else R.string.action_pin),
                                    modifier = Modifier.alpha(pinAlpha)
                                )
                            },
                            shape = MenuDefaults.standaloneItemShape,
                            leadingIcon = {
                                // Unpin shows the pin with a slash, the same glyph as the
                                // notification's own Unpin action.
                                Icon(
                                    painter = if (note.isPinned) painterResource(R.drawable.ic_unpin)
                                              else rememberVectorPainter(Icons.Rounded.PushPin),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(MenuDefaults.LeadingIconSize)
                                        .alpha(pinAlpha)
                                )
                            },
                            onClick = {
                                if (pinBlocked) haptics.reject() else haptics.toggle(on = !note.isPinned)
                                menuExpanded = false
                                onPinClick()
                            }
                        )
                    }
                    // Its own group: the gap between groups sets the destructive action apart.
                    DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 1, count = 2)) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            shape = MenuDefaults.standaloneItemShape,
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(MenuDefaults.LeadingIconSize)
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            ),
                            onClick = {
                                haptics.longPress()
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    }
}
