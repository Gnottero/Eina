package com.eina.app.ui.routine

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandRow
import com.eina.app.ui.components.RowLeadingTile
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.primaryCategoryFor

/**
 * The routines, as rows of a single island: a title, a line telling what the list is for, and one
 * low row per routine.
 *
 * They used to be a stack of white cards, one shadow and 12dp of air each, holding a name and a
 * grey line of exercise names. The list is short and always the same shape, so the island holds it
 * whole and the eye runs down it instead of counting rectangles.
 */
@Composable
fun RoutineListSection(
    routines: List<RoutineCardUi>,
    onStartSession: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onDeleteRoutine: (RoutineEntity) -> Unit,
    onExportRoutine: (Long, (String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    startBlocked: Boolean = false
) {
    val island = EinaTheme.island

    if (routines.isEmpty()) {
        IslandEmptyState(
            modifier = modifier,
            title = stringResource(R.string.routine_empty_title),
            description = stringResource(R.string.routine_empty_description),
            icon = Icons.AutoMirrored.Outlined.ListAlt
        )
        return
    }

    IslandCard(
        modifier = modifier.fillMaxWidth(),
        shape = IslandShape,
        contentPadding = PaddingValues(bottom = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Column(
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.lg,
                bottom = Spacing.xs
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.workout_your_routines),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.workout_routines_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }
        Column(modifier = Modifier.padding(horizontal = Spacing.sm)) {
            routines.forEach { card ->
                RoutineRow(
                    card = card,
                    // Starting a routine creates a NEW session: passing routine.id straight through
                    // opened the session with that id, i.e. an old recorded workout.
                    onStart = { onStartSession(card.routine.id) },
                    onEdit = { onEditRoutine(card.routine.id) },
                    onDelete = { onDeleteRoutine(card.routine) },
                    onExport = { onExported -> onExportRoutine(card.routine.id, onExported) },
                    startEnabled = !startBlocked
                )
            }
        }
    }
}

/**
 * Routine row: a square tinted with the muscle group the routine works most, the name, the count of
 * exercises and sets, and the round button that starts it.
 *
 * The round button stays even though the design draws a chevron there: starting a routine is the
 * one thing this screen exists for, and burying it one screen deep would cost a tap on every
 * workout. Tapping the row still opens the routine, and the long press still holds the rest.
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
    val routine: RoutineEntity = card.routine
    val name = routine.name.ifBlank { stringResource(R.string.routine_unnamed) }
    val color = primaryCategoryFor(listOfNotNull(card.dominantMuscle)).color
    var actionsOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val exportFailed = stringResource(R.string.routine_export_failed)

    IslandRow(
        title = name,
        titleStyle = MaterialTheme.typography.titleMedium,
        // Routine names are written by hand and run long ("Allenamento - Pausa pranzo"); one line
        // cut most of them mid-word.
        titleMaxLines = 2,
        subtitle = summaryLine(card),
        // Tapping the row opens the routine; only the round button on the right starts it.
        onClick = onEdit,
        onLongClick = { actionsOpen = true },
        leading = {
            RowLeadingTile(color = color) {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailing = {
            IslandIconButton(
                icon = Icons.Outlined.PlayArrow,
                contentDescription = stringResource(R.string.routine_start_cd, name),
                onClick = onStart,
                enabled = startEnabled,
                size = 40.dp,
                containerColor = if (startEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    island.sunken
                },
                contentColor = if (startEnabled) Color.White else island.textSecondary
            )
        }
    )

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
