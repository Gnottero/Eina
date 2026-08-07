package com.eina.app.ui.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
                title = if (routineId == 0L) "Nuova routine" else "Routine",
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
                label = "Nome routine",
                modifier = Modifier.fillMaxWidth()
            )
            IslandTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = "Note (opzionale)",
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Playlist", style = MaterialTheme.typography.titleMedium)
            PlaylistTypeDropdown(
                selected = uiState.linkedPlaylistType,
                onSelected = viewModel::onPlaylistTypeChange
            )
            IslandTextField(
                value = uiState.linkedPlaylistUri,
                onValueChange = viewModel::onPlaylistUriChange,
                label = "Link playlist (opzionale)",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                // Il tasto "Riproduci" e' stato spostato nell'allenamento in corso: la musica
                // serve mentre ci si allena, non mentre si compila la scheda.
                text = "La playlist si avvia dalla schermata dell'allenamento.",
                style = MaterialTheme.typography.bodySmall,
                color = EinaTheme.island.textSecondary
            )
        }

        SectionHeader(title = "Esercizi")

        if (routineExercises.isEmpty()) {
            IslandEmptyState(
                title = "Nessun esercizio",
                description = "Aggiungi esercizi e imposta serie, ripetizioni, peso e recupero target."
            )
        } else {
            routineExercises.forEach { routineExercise ->
                RoutineExerciseRow(
                    routineExercise = routineExercise,
                    exerciseName = exerciseNames[routineExercise.exerciseId] ?: "...",
                    onUpdate = { sets, reps, weight, rest ->
                        viewModel.updateTargets(routineExercise, sets, reps, weight, rest)
                    },
                    onNotesChange = { notes -> viewModel.updateNotes(routineExercise, notes) },
                    onRemove = { viewModel.removeExercise(routineExercise) }
                )
            }
        }

        IslandSecondaryButton(
            text = "Aggiungi esercizio",
            icon = Icons.Outlined.Add,
            onClick = onPickExercise,
            modifier = Modifier.fillMaxWidth()
        )

        IslandButton(
            text = "Salva routine",
            onClick = { viewModel.save(onSaved) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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
                label = "Serie",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            IslandNumberField(
                value = reps,
                onValueChange = { reps = it; commit() },
                label = "Reps",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            IslandNumberField(
                value = weight,
                onValueChange = { weight = sanitizeWeightInput(weight, it); commit() },
                label = "Kg",
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
                .clickable { hapticTap(); restSheetOpen = true }
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Recupero ${formatClock(routineExercise.restSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary
            )
        }
    }

    if (actionsOpen) {
        IslandBottomSheet(onDismiss = { actionsOpen = false }, title = exerciseName) {
            SheetActionRow(
                icon = Icons.AutoMirrored.Outlined.Notes,
                label = if (routineExercise.notes.isNullOrBlank()) "Aggiungi nota" else "Modifica nota",
                description = routineExercise.notes?.takeIf { it.isNotBlank() },
                onClick = { actionsOpen = false; notesSheetOpen = true }
            )
            SheetActionRow(
                icon = Icons.Outlined.Timer,
                label = "Tempo di recupero",
                description = formatClock(routineExercise.restSeconds),
                onClick = { actionsOpen = false; restSheetOpen = true }
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = "Rimuovi esercizio",
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
            description = "Recupero proposto fra le serie di questo esercizio."
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

    IslandBottomSheet(onDismiss = onDismiss, title = "Nota su $exerciseName") {
        IslandTextField(
            value = text,
            onValueChange = { text = it },
            label = "Nota",
            singleLine = false,
            modifier = Modifier.fillMaxWidth()
        )
        IslandButton(
            text = "Salva nota",
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
        null -> "Nessuna playlist"
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        IslandTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = "Servizio",
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Nessuna playlist") }, onClick = { onSelected(null); expanded = false })
            DropdownMenuItem(text = { Text("Spotify") }, onClick = { onSelected(PlaylistType.SPOTIFY); expanded = false })
            DropdownMenuItem(text = { Text("YouTube Music") }, onClick = { onSelected(PlaylistType.YOUTUBE_MUSIC); expanded = false })
        }
    }
}
