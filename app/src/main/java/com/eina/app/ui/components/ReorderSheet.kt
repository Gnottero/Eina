package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.eina.app.R
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import kotlin.math.roundToInt

/**
 * A reorderable row. The key is a string and not an id: a superset travels as a single block
 * (see [com.eina.app.domain.Superset]) and has no id of its own.
 */
data class ReorderRow(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    /** Superset colour, drawn as the row border when the row is a round. */
    val tint: Color? = null
)

/**
 * Sheet to reorder exercises by dragging, used by both the routine editor and the workout screen.
 * The whole list is under the thumb and the order is written once, on confirmation.
 *
 * Rows have a fixed height, so the target position is the drag distance divided by that height,
 * with no need to measure each row while dragging.
 */
@Composable
fun ReorderSheet(
    title: String,
    rows: List<ReorderRow>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() + Spacing.xs.toPx() }

    var order by remember(rows) { mutableStateOf(rows) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    IslandBottomSheet(onDismiss = onDismiss, title = title, scrollable = true) {
        Text(
            text = stringResource(R.string.reorder_description),
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            order.forEachIndexed { index, row ->
                // key(): without it the composition slot stays the index, so the gesture detector
                // would move to another row and the drag would break as soon as the row moves.
                key(row.key) {
                    val dragging = row.key == draggedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            // The dragged row floats above the others, which reorder underneath so
                            // the drop position is visible.
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                            .clip(TileShape)
                            .background(if (dragging) island.sunken else island.sunkenSoft)
                            .then(
                                if (row.tint == null) Modifier
                                else Modifier.border(2.dp, row.tint, TileShape)
                            )
                            .pointerInput(row.key) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        hapticTap()
                                        draggedKey = row.key
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragOffset += delta.y
                                        // Past half a row the item swaps immediately and the
                                        // leftover offset stays under the finger; reordering only
                                        // on release would hide where it lands.
                                        val shift = (dragOffset / rowHeightPx).roundToInt()
                                        val from = order.indexOfFirst { it.key == row.key }
                                        val to = (from + shift).coerceIn(0, order.size - 1)
                                        if (to != from) {
                                            order = order.toMutableList()
                                                .apply { add(to, removeAt(from)) }
                                            dragOffset -= (to - from) * rowHeightPx
                                            hapticTap()
                                        }
                                    },
                                    onDragEnd = { draggedKey = null; dragOffset = 0f },
                                    onDragCancel = { draggedKey = null; dragOffset = 0f }
                                )
                            }
                            .padding(horizontal = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = island.textSecondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            row.subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = island.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.DragHandle,
                                contentDescription = stringResource(R.string.reorder_handle_cd),
                                tint = island.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        IslandButton(
            text = stringResource(R.string.action_done),
            onClick = { onConfirm(order.map { it.key }); onDismiss() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val ROW_HEIGHT = 60.dp
