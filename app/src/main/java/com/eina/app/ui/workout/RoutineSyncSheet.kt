package com.eina.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.RoutineChange
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.formatClock
import com.eina.app.ui.library.localized
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/**
 * Asked when a session started from a routine but did not follow it: was the routine wrong, or was
 * it just today? The question is asked once, with the list of what changed — without it, updating
 * the routine meant redoing the same edits by hand in the editor.
 *
 * Doing nothing is the likelier choice and sits on the secondary button, but neither option is
 * destructive: the routine can always be corrected by hand.
 */
@Composable
fun RoutineSyncSheet(
    routineName: String?,
    changes: List<RoutineChange>,
    onUpdate: () -> Unit,
    onKeep: () -> Unit
) {
    val island = EinaTheme.island
    IslandBottomSheet(
        // The sheet does not dismiss on an outside tap: the question must be answered, or the user
        // is left on a closed workout without knowing what happened to the routine.
        onDismiss = onKeep,
        title = stringResource(R.string.routine_sync_title),
        scrollable = true
    ) {
        Text(
            // Second of the two questions that close a workout; the first one said so too.
            text = routineName
                ?.let { stringResource(R.string.finish_step_two, it) }
                ?: stringResource(R.string.finish_step_two_generic),
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(island.sunken)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            changes.forEach { change -> ChangeRow(change) }
        }

        Text(
            text = stringResource(R.string.routine_sync_targets_note),
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary
        )

        // Side by side rather than stacked: the two answers are equal in weight, and one above the
        // other read as a recommendation and its afterthought.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            IslandSecondaryButton(
                text = stringResource(R.string.routine_sync_keep),
                onClick = onKeep,
                modifier = Modifier.weight(1f)
            )
            IslandButton(
                text = stringResource(R.string.routine_sync_apply),
                icon = Icons.Outlined.Check,
                onClick = onUpdate,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChangeRow(change: RoutineChange) {
    val island = EinaTheme.island
    val icon = when (change) {
        is RoutineChange.Added -> Icons.Outlined.Add
        is RoutineChange.Removed -> Icons.Outlined.Delete
        is RoutineChange.SetsChanged -> Icons.Outlined.Repeat
        is RoutineChange.RestChanged -> Icons.Outlined.Timer
        RoutineChange.Reordered -> Icons.Outlined.SwapVert
    }
    val text = when (change) {
        is RoutineChange.Added -> stringResource(R.string.routine_sync_added, change.name.localized())
        is RoutineChange.Removed -> stringResource(R.string.routine_sync_removed, change.name.localized())
        is RoutineChange.SetsChanged ->
            stringResource(R.string.routine_sync_sets, change.name.localized(), change.from, change.to)
        is RoutineChange.RestChanged -> stringResource(
            R.string.routine_sync_rest,
            change.name.localized(),
            formatClock(change.fromSeconds),
            formatClock(change.toSeconds)
        )
        RoutineChange.Reordered -> stringResource(R.string.routine_sync_reordered)
    }
    // The icon sits in a tinted disc and takes the colour of what happened: removed is the only
    // red one. A column of identical grey glyphs said nothing the sentence did not already say.
    val tint = when (change) {
        is RoutineChange.Removed -> DestructiveRed
        is RoutineChange.Added -> MaterialTheme.colorScheme.primary
        else -> island.textSecondary
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(PillShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
