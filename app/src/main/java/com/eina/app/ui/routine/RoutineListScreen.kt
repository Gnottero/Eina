package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/**
 * Routine list. Deliberately non-lazy: routines are few and the enclosing screen already scrolls
 * (a LazyColumn nested in a vertical scroll cannot be measured).
 */
@Composable
fun RoutineListScreen(
    onStartSession: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    startBlocked: Boolean = false,
    viewModel: RoutineListViewModel = koinViewModel()
) {
    val routines by viewModel.routines.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (routines.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.routine_empty_title),
                description = stringResource(R.string.routine_empty_description),
                icon = Icons.AutoMirrored.Outlined.ListAlt
            )
        } else {
            routines.forEach { card ->
                RoutineRow(
                    card = card,
                    // Starting a routine creates a NEW session: passing routine.id straight through
                    // opened the session with that id, i.e. an old recorded workout.
                    onStart = { viewModel.startSession(card.routine.id, onStartSession) },
                    onEdit = { onEditRoutine(card.routine.id) },
                    onDelete = { viewModel.deleteRoutine(card.routine) },
                    onExport = { onExported -> viewModel.exportRoutine(card.routine.id, onExported) },
                    startEnabled = !startBlocked
                )
            }
        }
    }
}

/**
 * Minimal routine card: name, one content line ("5 exercises · 18 sets"), the exercise list in grey
 * and a single round start button. Editing and deletion live in the long-press sheet.
 */
@Composable
private fun RoutineRow(
    card: RoutineCardUi,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExport: ((String?) -> Unit) -> Unit,
    startEnabled: Boolean
) {
    val context = LocalContext.current
    val island = EinaTheme.island
    val locale = currentLocale()
    val routine: RoutineEntity = card.routine
    val name = routine.name.ifBlank { stringResource(R.string.routine_unnamed) }
    var actionsOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val exportFailed = stringResource(R.string.routine_export_failed)

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        // Tapping the card opens the routine; only the round button on the right starts it.
        onClick = onEdit,
        onLongClick = { actionsOpen = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summaryLine(card),
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    maxLines = 1
                )
            }
            IslandIconButton(
                icon = Icons.Outlined.PlayArrow,
                contentDescription = stringResource(R.string.routine_start_cd, name),
                onClick = onStart,
                enabled = startEnabled,
                size = 48.dp,
                containerColor = if (startEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    island.sunken
                },
                contentColor = if (startEnabled) Color.White else island.textSecondary
            )
        }

        if (card.exerciseNames.isNotEmpty()) {
            Text(
                text = card.exerciseNames.joinToString(" · ") { it.localized(locale) },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        routine.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (actionsOpen) {
        IslandBottomSheet(onDismiss = { actionsOpen = false }, title = name) {
            SheetActionRow(
                icon = Icons.Outlined.EditNote,
                label = stringResource(R.string.routine_edit),
                onClick = { actionsOpen = false; onEdit() }
            )
            // Export targets whoever writes a routine for someone else: the file goes wherever the
            // user sends it, never to a service of ours.
            SheetActionRow(
                icon = Icons.Outlined.IosShare,
                label = stringResource(R.string.routine_export),
                description = stringResource(R.string.routine_export_description),
                onClick = {
                    actionsOpen = false
                    onExport { json ->
                        if (json == null) {
                            Toast.makeText(context, exportFailed, Toast.LENGTH_SHORT).show()
                        } else {
                            shareRoutineFile(context, routineFileName(name), json)
                        }
                    }
                }
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.routine_delete),
                description = stringResource(R.string.routine_delete_description),
                destructive = true,
                onClick = { actionsOpen = false; confirmDelete = true }
            )
        }
    }

    if (confirmDelete) {
        // Deletion cannot be undone, so it is confirmed before touching the database.
        IslandAlertDialog(
            title = stringResource(R.string.routine_delete_confirm_title, name),
            text = stringResource(R.string.routine_delete_confirm_text),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            dismissLabel = stringResource(R.string.action_cancel),
            onDismiss = { confirmDelete = false }
        )
    }
}

/** "5 exercises · 18 sets", or the prompt to fill the routine while it is still empty. */
@Composable
private fun summaryLine(card: RoutineCardUi): String {
    if (card.exerciseCount == 0) return stringResource(R.string.routine_no_exercises)
    val exercises = pluralStringResource(R.plurals.exercise_count, card.exerciseCount, card.exerciseCount)
    val sets = pluralStringResource(R.plurals.set_count, card.setCount, card.setCount)
    return "$exercises · $sets"
}
