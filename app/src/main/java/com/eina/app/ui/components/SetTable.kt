package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.usesDistance
import com.eina.app.data.db.usesDuration
import com.eina.app.data.db.usesWeight
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/**
 * Header of the set table, shared by the workout screen and the routine editor.
 *
 * A routine has nothing to compare against and nothing to check off, so the previous column and
 * the completion button are switched off; the rest is the same table.
 */
@Composable
fun SetTableHeader(
    weightType: WeightType,
    showPrevious: Boolean = true,
    trailingSlot: Boolean = true
) {
    Row(
        // Same horizontal inset as the table rows (see SetRow): otherwise the header columns start
        // 4dp further left and the labels no longer sit above their fields.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TableLabel(stringResource(R.string.table_set), Modifier.width(40.dp))
        if (showPrevious) {
            TableLabel(stringResource(R.string.table_previous), Modifier.weight(previousColumnWeight(weightType)))
        }
        // With no load to type the kg column is dropped and its space goes to the reps. For
        // distance exercises the same decimal field holds kilometres.
        if (weightType.usesWeight) {
            TableLabel(stringResource(R.string.table_kg), Modifier.weight(1f))
        } else if (weightType.usesDistance) {
            TableLabel(stringResource(R.string.table_km), Modifier.weight(1f))
        }
        TableLabel(
            stringResource(
                when {
                    weightType.usesDuration -> R.string.table_seconds
                    weightType.usesDistance -> R.string.table_minutes
                    else -> R.string.table_reps
                }
            ),
            Modifier.weight(1f)
        )
        if (trailingSlot) {
            Box(Modifier.size(42.dp))
        }
    }
}

/**
 * Width of the "previous" column. For distance the summary is twice as long ("5.2km·30" against
 * "60kg×8") and got clipped in the narrow column.
 */
fun previousColumnWeight(weightType: WeightType): Float =
    if (weightType.usesDistance) 1.7f else 1.1f

/**
 * Table row deleted by dragging it to the left.
 *
 * Long press used to be the only way to remove a set, and on an exercise without the kg column the
 * numeric fields take nearly the whole row, leaving millimetres of border for the gesture. A
 * horizontal swipe is not intercepted by the fields, and the long press stays for the other
 * actions.
 *
 * The threshold is half the row, so a set is not deleted by brushing past it while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteSetRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hapticTap = LocalHapticTap.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                hapticTap()
                onDelete()
            }
            // The state is never confirmed: the row disappears because the data does, and a failed
            // deletion leaves it back in place instead of stranded off screen.
            false
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // The red only shows while dragging: drawn always, it tinted the resting row pink,
            // which has no background of its own.
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(TileShape)
                        .background(DestructiveRed.copy(alpha = 0.12f))
                        .padding(horizontal = Spacing.lg),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.active_delete_set),
                        tint = DestructiveRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = modifier,
        content = {
            // The sliding row carries its own background, or the red shows through it.
            Box(
                modifier = Modifier
                    .clip(TileShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                content()
            }
        }
    )
}

@Composable
fun TableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EinaTheme.island.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Numeric field of the set table: no label, grey placeholder with the target or last-time value,
 * which stays a suggestion and not a recorded datum.
 */
@Composable
fun SetValueField(
    value: String,
    placeholder: String?,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(PillShape)
            .background(island.sunkenSoft)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                        color = island.textSecondary
                    )
                }
                inner()
            }
        }
    )
}
