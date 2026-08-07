package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandNumberField
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.ui.theme.EinaTheme
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
    val context = LocalContext.current

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
            if (uiState.linkedPlaylistType != null && uiState.linkedPlaylistUri.isNotBlank()) {
                IslandSecondaryButton(
                    text = "Riproduci",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = {
                        launchPlaylist(context, uiState.linkedPlaylistUri, uiState.linkedPlaylistType!!)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
    onRemove: () -> Unit
) {
    var sets by remember(routineExercise.id) { mutableStateOf(routineExercise.targetSets.toString()) }
    var reps by remember(routineExercise.id) { mutableStateOf(routineExercise.targetReps.toString()) }
    var weight by remember(routineExercise.id) { mutableStateOf(routineExercise.targetWeight?.toString() ?: "") }
    var rest by remember(routineExercise.id) { mutableStateOf(routineExercise.restSeconds.toString()) }

    fun commit() {
        onUpdate(
            sets.toIntOrNull() ?: routineExercise.targetSets,
            reps.toIntOrNull() ?: routineExercise.targetReps,
            weight.toDoubleOrNull(),
            rest.toIntOrNull() ?: routineExercise.restSeconds
        )
    }

    IslandCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                exerciseName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IslandIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Rimuovi esercizio",
                onClick = onRemove,
                containerColor = EinaTheme.island.sunken,
                size = 38.dp
            )
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
            IslandNumberField(
                value = rest,
                onValueChange = { rest = it; commit() },
                label = "Rec s",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
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
