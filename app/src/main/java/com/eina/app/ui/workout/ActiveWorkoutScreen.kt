package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.ui.components.BottomTimerBar
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.EinaCard
import com.eina.app.ui.theme.AccentPrimary
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(sessionId) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allenamento in corso") },
                actions = {
                    TextButton(onClick = { viewModel.finishWorkout(onFinished) }) {
                        Text("Termina")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "Aggiungi esercizio")
            }
        },
        bottomBar = {
            state.timer?.let { timer ->
                BottomTimerBar(
                    remainingSeconds = timer.remainingSeconds,
                    onMinus15 = { viewModel.adjustTimer(-15) },
                    onPlus15 = { viewModel.adjustTimer(15) },
                    onSkip = { viewModel.skipTimer() }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            items(state.exercises, key = { it.workoutExerciseId }) { exercise ->
                val index = state.exercises.indexOf(exercise)
                ExerciseCard(
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.size - 1,
                    onMoveUp = { viewModel.moveExercise(exercise.workoutExerciseId, -1) },
                    onMoveDown = { viewModel.moveExercise(exercise.workoutExerciseId, 1) },
                    onRemove = { viewModel.removeExercise(exercise.workoutExerciseId) },
                    onAddSet = { viewModel.addSet(exercise.workoutExerciseId) },
                    onRemoveSet = { setId -> viewModel.removeSet(exercise.workoutExerciseId, setId) },
                    onSetValuesChange = { setId, reps, weight ->
                        viewModel.updateSetValues(exercise.workoutExerciseId, setId, reps, weight)
                    },
                    onCompleteSet = { setId -> viewModel.completeSet(exercise.workoutExerciseId, setId) }
                )
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            exercises = state.availableExercises,
            onPick = {
                viewModel.addExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: SessionExerciseUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onSetValuesChange: (Long, Int?, Double?) -> Unit,
    onCompleteSet: (Long) -> Unit
) {
    EinaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Sposta su")
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Sposta giu'")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Rimuovi esercizio")
                }
            }
        }

        if (exercise.lastTimeSets.isNotEmpty()) {
            Text(
                text = "Ultima volta: " + exercise.lastTimeSets.joinToString(" · ") { s ->
                    val w = s.weight?.let { "${it}kg " } ?: ""
                    "$w${s.actualReps ?: "-"} rip"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        exercise.sets.forEach { set ->
            SetRow(
                set = set,
                onValuesChange = { reps, weight -> onSetValuesChange(set.id, reps, weight) },
                onComplete = { onCompleteSet(set.id) },
                onRemove = { onRemoveSet(set.id) }
            )
        }

        TextButton(onClick = onAddSet) {
            Text("+ Serie")
        }
    }
}

@Composable
private fun SetRow(
    set: SessionSetUi,
    onValuesChange: (Int?, Double?) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text("#${set.setIndex + 1}", modifier = Modifier.width(28.dp))

        OutlinedTextField(
            value = set.weight?.toString() ?: "",
            onValueChange = { text -> onValuesChange(set.actualReps, text.toDoubleOrNull()) },
            label = { Text("kg") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(90.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = set.actualReps?.toString() ?: "",
            onValueChange = { text -> onValuesChange(text.toIntOrNull(), set.weight) },
            label = { Text("rip") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp),
            singleLine = true
        )

        if (set.isPR) {
            EinaBadge(text = "PR", color = AccentPrimary)
        }

        Spacer(modifier = Modifier.width(0.dp))

        IconButton(onClick = onComplete, enabled = set.completedAt == null) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Completa serie",
                tint = if (set.completedAt != null) AccentPrimary else MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Close, contentDescription = "Rimuovi serie")
        }
    }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<ExerciseEntity>,
    onPick: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi esercizio") },
        text = {
            if (exercises.isEmpty()) {
                Text("Nessun esercizio in libreria. Completa la Fase 3 (seed libreria) per popolarla.")
            } else {
                LazyColumn {
                    items(exercises, key = { it.id }) { exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            modifier = Modifier.padding(vertical = 0.dp),
                            trailingContent = {
                                Button(onClick = { onPick(exercise) }) { Text("Aggiungi") }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}
