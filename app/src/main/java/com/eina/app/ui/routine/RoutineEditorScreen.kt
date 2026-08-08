package com.eina.app.ui.routine

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandNumberField
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.RestTimeSheet
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.formatClock
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.localized
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onPickExercise: () -> Unit,
    pickedExerciseId: Long? = null,
    onExercisePickedConsumed: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSaved: () -> Unit = {},
    viewModel: RoutineEditorViewModel = koinViewModel(parameters = { parametersOf(routineId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val routineExercises by viewModel.routineExercises.collectAsState()
    val exerciseNames by viewModel.exerciseNames.collectAsState()

    LaunchedEffect(pickedExerciseId) {
        if (pickedExerciseId != null) {
            viewModel.addExercise(pickedExerciseId)
            onExercisePickedConsumed()
        }
    }

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(if (routineId == 0L) R.string.routine_editor_new_title else R.string.routine_editor_title),
                subtitle = uiState.name.takeIf { it.isNotBlank() },
                onBack = onBack?.let { back -> { viewModel.discardIfEmpty(back) } }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            IslandTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_routine_name),
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.field_notes),
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.routine_playlist), style = MaterialTheme.typography.titleMedium)
            PlaylistTypeDropdown(
                selected = uiState.linkedPlaylistType,
                onSelected = viewModel::onPlaylistTypeChange
            )
            IslandTextField(
                value = uiState.linkedPlaylistUri,
                onValueChange = viewModel::onPlaylistUriChange,
                label = stringResource(R.string.field_playlist_link),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                // Il tasto "Riproduci" e' stato spostato nell'allenamento in corso: la musica
                // serve mentre ci si allena, non mentre si compila la scheda.
                text = stringResource(R.string.routine_playlist_hint),
                style = MaterialTheme.typography.bodySmall,
                color = EinaTheme.island.textSecondary
            )
        }

        SectionHeader(title = stringResource(R.string.section_exercises))

        if (routineExercises.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.routine_editor_empty_title),
                description = stringResource(R.string.routine_editor_empty_description)
            )
        } else {
            routineExercises.forEach { routineExercise ->
                RoutineExerciseRow(
                    routineExercise = routineExercise,
                    exerciseName = exerciseNames[routineExercise.exerciseId]?.localized() ?: "…",
                    onUpdate = { sets, reps, weight, rest ->
                        viewModel.updateTargets(routineExercise, sets, reps, weight, rest)
                    },
                    onNotesChange = { notes -> viewModel.updateNotes(routineExercise, notes) },
                    onRemove = { viewModel.removeExercise(routineExercise) }
                )
            }
        }

        IslandSecondaryButton(
            text = stringResource(R.string.action_add_exercise),
            icon = Icons.Outlined.Add,
            onClick = onPickExercise,
            modifier = Modifier.fillMaxWidth()
        )

        IslandButton(
            text = stringResource(R.string.action_save_routine),
            onClick = { viewModel.save(onSaved) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineExerciseRow(
    routineExercise: RoutineExerciseEntity,
    exerciseName: String,
    onUpdate: (Int, Int, Double?, Int) -> Unit,
    onNotesChange: (String?) -> Unit,
    onRemove: () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    var sets by remember(routineExercise.id) { mutableStateOf(routineExercise.targetSets.toString()) }
    var reps by remember(routineExercise.id) { mutableStateOf(routineExercise.targetReps.toString()) }
    var weight by remember(routineExercise.id) { mutableStateOf(routineExercise.targetWeight?.toString() ?: "") }
    var actionsOpen by remember { mutableStateOf(false) }
    var restSheetOpen by remember { mutableStateOf(false) }
    var notesSheetOpen by remember { mutableStateOf(false) }

    fun commit(restSeconds: Int = routineExercise.restSeconds) {
        onUpdate(
            sets.toIntOrNull() ?: routineExercise.targetSets,
            reps.toIntOrNull() ?: routineExercise.targetReps,
            weight.toDoubleOrNull(),
            restSeconds
        )
    }

    // Come nell'allenamento: nessun tasto di servizio sulla card, le azioni stanno nel foglio
    // che si apre col tocco lungo.
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        onLongClick = { actionsOpen = true }
    ) {
        Text(exerciseName, style = MaterialTheme.typography.titleMedium)

        routineExercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            IslandNumberField(
                value = sets,
                onValueChange = { sets = it; commit() },
                label = stringResource(R.string.field_sets),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            IslandNumberField(
                value = reps,
                onValueChange = { reps = it; commit() },
                label = stringResource(R.string.field_reps),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            IslandNumberField(
                value = weight,
                onValueChange = { weight = sanitizeWeightInput(weight, it); commit() },
                label = stringResource(R.string.field_kg),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // Il recupero non e' un numero da digitare: si sceglie coi rulli, come una sveglia.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .clip(PillShape)
                .background(island.sunken)
                // Come sulla card: il tocco lungo apre le azioni anche partendo dal chip.
                .combinedClickable(
                    onLongClick = { hapticTap(); actionsOpen = true },
                    onClick = { hapticTap(); restSheetOpen = true }
                )
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.rest_label, formatClock(routineExercise.restSeconds)),
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary
            )
        }
    }

    if (actionsOpen) {
        IslandBottomSheet(onDismiss = { actionsOpen = false }, title = exerciseName) {
            SheetActionRow(
                icon = Icons.AutoMirrored.Outlined.Notes,
                label = stringResource(if (routineExercise.notes.isNullOrBlank()) R.string.note_add else R.string.note_edit),
                description = routineExercise.notes?.takeIf { it.isNotBlank() },
                onClick = { actionsOpen = false; notesSheetOpen = true }
            )
            SheetActionRow(
                icon = Icons.Outlined.Timer,
                label = stringResource(R.string.rest_time_title),
                description = formatClock(routineExercise.restSeconds),
                onClick = { actionsOpen = false; restSheetOpen = true }
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.action_remove_exercise),
                destructive = true,
                onClick = { actionsOpen = false; onRemove() }
            )
        }
    }

    if (restSheetOpen) {
        RestTimeSheet(
            currentSeconds = routineExercise.restSeconds,
            onConfirm = { seconds -> commit(restSeconds = seconds) },
            onDismiss = { restSheetOpen = false },
            description = stringResource(R.string.rest_description_routine)
        )
    }

    if (notesSheetOpen) {
        RoutineNotesSheet(
            exerciseName = exerciseName,
            notes = routineExercise.notes.orEmpty(),
            onSave = onNotesChange,
            onDismiss = { notesSheetOpen = false }
        )
    }
}

/** Nota della routine: promemoria sull'esecuzione, ereditata da ogni allenamento che la avvia. */
@Composable
private fun RoutineNotesSheet(
    exerciseName: String,
    notes: String,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(notes) { mutableStateOf(notes) }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.note_sheet_title, exerciseName)) {
        IslandTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.field_note),
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )
        IslandButton(
            text = stringResource(R.string.action_save_note),
            onClick = { onSave(text); onDismiss() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTypeDropdown(selected: PlaylistType?, onSelected: (PlaylistType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        PlaylistType.SPOTIFY -> "Spotify"
        PlaylistType.YOUTUBE_MUSIC -> "YouTube Music"
        null -> stringResource(R.string.playlist_none)
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        IslandTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.playlist_service),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.playlist_none)) }, onClick = { onSelected(null); expanded = false })
            DropdownMenuItem(text = { Text("Spotify") }, onClick = { onSelected(PlaylistType.SPOTIFY); expanded = false })
            DropdownMenuItem(text = { Text("YouTube Music") }, onClick = { onSelected(PlaylistType.YOUTUBE_MUSIC); expanded = false })
        }
    }
}
