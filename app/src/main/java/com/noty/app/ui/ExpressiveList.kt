package com.noty.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Material 3 expressive "connected pill" grouped-list shapes: items in a group share
 * tight inner corners while the first/last get large outer corners, separated by a
 * small gap instead of dividers.
 */
enum class SegmentPosition { Single, First, Middle, Last }

val GroupOuterRadius = 24.dp
val GroupInnerRadius = 4.dp

fun segmentShape(position: SegmentPosition): Shape = when (position) {
    SegmentPosition.Single -> RoundedCornerShape(GroupOuterRadius)
    SegmentPosition.First -> RoundedCornerShape(
        topStart = GroupOuterRadius, topEnd = GroupOuterRadius,
        bottomStart = GroupInnerRadius, bottomEnd = GroupInnerRadius
    )
    SegmentPosition.Middle -> RoundedCornerShape(GroupInnerRadius)
    SegmentPosition.Last -> RoundedCornerShape(
        topStart = GroupInnerRadius, topEnd = GroupInnerRadius,
        bottomStart = GroupOuterRadius, bottomEnd = GroupOuterRadius
    )
}

fun segmentPositionFor(index: Int, count: Int): SegmentPosition = when {
    count == 1 -> SegmentPosition.Single
    index == 0 -> SegmentPosition.First
    index == count - 1 -> SegmentPosition.Last
    else -> SegmentPosition.Middle
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}
