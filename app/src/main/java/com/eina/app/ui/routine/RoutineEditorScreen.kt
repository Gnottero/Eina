package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.ui.components.EinaCard
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onPickExercise: () -> Unit,
    pickedExerciseId: Long? = null,
    onExercisePickedConsumed: () -> Unit = {},
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text("Routine", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nome routine") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Note (opzionale)") },
            modifier = Modifier.fillMaxWidth()
        )

        PlaylistTypeDropdown(selected = uiState.linkedPlaylistType, onSelected = viewModel::onPlaylistTypeChange)

        OutlinedTextField(
            value = uiState.linkedPlaylistUri,
            onValueChange = viewModel::onPlaylistUriChange,
            label = { Text("Link playlist (opzionale)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.linkedPlaylistType != null && uiState.linkedPlaylistUri.isNotBlank()) {
            OutlinedButton(onClick = {
                launchPlaylist(context, uiState.linkedPlaylistUri, uiState.linkedPlaylistType!!)
            }) {
                Text("Riproduci")
            }
        }

        Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
            Text("Salva routine")
        }

        HorizontalDivider()

        Text("Esercizi", style = MaterialTheme.typography.titleMedium)

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

        OutlinedButton(onClick = onPickExercise, modifier = Modifier.fillMaxWidth()) {
            Text("Aggiungi esercizio")
        }
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

    EinaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onRemove) { Text("Rimuovi") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it; commit() },
                    label = { Text("Serie") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it; commit() },
                    label = { Text("Reps") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it; commit() },
                    label = { Text("Peso kg") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rest,
                    onValueChange = { rest = it; commit() },
                    label = { Text("Recupero s") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
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
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Playlist") },
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
