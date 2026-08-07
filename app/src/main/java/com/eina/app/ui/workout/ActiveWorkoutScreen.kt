package com.eina.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.ui.components.BottomTimerBar
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandNumberField
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.theme.AccentPrimary
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(sessionId) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ScreenHeader(
                title = "Allenamento",
                subtitle = "${state.exercises.size} esercizi in sessione",
                trailing = {
                    IslandButton(
                        text = "Termina",
                        onClick = { viewModel.finishWorkout(onFinished) }
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    top = Spacing.sm,
                    bottom = 200.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                if (state.exercises.isEmpty()) {
                    item {
                        IslandEmptyState(
                            title = "Sessione vuota",
                            description = "Aggiungi il primo esercizio per iniziare a registrare le serie."
                        )
                    }
                }

                itemsIndexed(state.exercises, key = { _, item -> item.workoutExerciseId }) { index, exercise ->
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

                item {
                    IslandSecondaryButton(
                        text = "Aggiungi esercizio",
                        icon = Icons.Outlined.Add,
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Il timer di recupero e' un'isola che galleggia sopra la lista, non una barra ancorata.
        state.timer?.let { timer ->
            BottomTimerBar(
                remainingSeconds = timer.remainingSeconds,
                totalSeconds = timer.totalSeconds,
                onMinus15 = { viewModel.adjustTimer(-15) },
                onPlus15 = { viewModel.adjustTimer(15) },
                onSkip = { viewModel.skipTimer() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
            )
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
    val island = EinaTheme.island

    IslandCard(modifier = Modifier.fillMaxWidth(), shape = IslandShape) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IslandIconButton(
                icon = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Sposta su",
                onClick = { if (canMoveUp) onMoveUp() },
                containerColor = island.sunken,
                contentColor = if (canMoveUp) MaterialTheme.colorScheme.onSurface else island.textSecondary,
                size = 34.dp
            )
            IslandIconButton(
                icon = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Sposta giu'",
                onClick = { if (canMoveDown) onMoveDown() },
                containerColor = island.sunken,
                contentColor = if (canMoveDown) MaterialTheme.colorScheme.onSurface else island.textSecondary,
                size = 34.dp
            )
            IslandIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Rimuovi esercizio",
                onClick = onRemove,
                containerColor = island.sunken,
                size = 34.dp
            )
        }

        if (exercise.lastTimeSets.isNotEmpty()) {
            Text(
                text = "Ultima volta: " + exercise.lastTimeSets.joinToString(" · ") { s ->
                    val w = s.weight?.let { "${it}kg " } ?: ""
                    "$w${s.actualReps ?: "-"} rip"
                },
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
        }

        exercise.sets.forEach { set ->
            SetRow(
                set = set,
                onValuesChange = { reps, weight -> onSetValuesChange(set.id, reps, weight) },
                onComplete = { onCompleteSet(set.id) },
                onRemove = { onRemoveSet(set.id) }
            )
        }

        IslandSecondaryButton(
            text = "Aggiungi serie",
            icon = Icons.Outlined.Add,
            onClick = onAddSet,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SetRow(
    set: SessionSetUi,
    onValuesChange: (Int?, Double?) -> Unit,
    onComplete: () -> Unit,
    onRemove: () -> Unit
) {
    val island = EinaTheme.island
    val completed = set.completedAt != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(
            modifier = Modifier.size(width = 28.dp, height = 48.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                "#${set.setIndex + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = island.textSecondary,
                modifier = Modifier.padding(bottom = Spacing.md)
            )
        }

        IslandNumberField(
            value = set.weight?.toString() ?: "",
            onValueChange = { text -> onValuesChange(set.actualReps, text.toDoubleOrNull()) },
            label = "Kg",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )

        IslandNumberField(
            value = set.actualReps?.toString() ?: "",
            onValueChange = { text -> onValuesChange(text.toIntOrNull(), set.weight) },
            label = "Rip",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        if (set.isPR) {
            Box(modifier = Modifier.padding(bottom = Spacing.md)) {
                EinaBadge(text = "PR", color = AccentPrimary, filled = true)
            }
        }

        IslandIconButton(
            icon = Icons.Outlined.Check,
            contentDescription = "Completa serie",
            onClick = { if (!completed) onComplete() },
            containerColor = if (completed) AccentPrimary else island.sunken,
            contentColor = if (completed) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
            size = 40.dp
        )

        IslandIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "Rimuovi serie",
            onClick = onRemove,
            containerColor = island.sunken,
            contentColor = island.textSecondary,
            size = 40.dp
        )
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
        shape = IslandShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Aggiungi esercizio", style = MaterialTheme.typography.titleLarge) },
        text = {
            if (exercises.isEmpty()) {
                Text("Nessun esercizio in libreria.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        IslandCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(Spacing.md),
                            elevation = 0.dp,
                            color = EinaTheme.island.sunken,
                            onClick = { onPick(exercise) }
                        ) {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}
