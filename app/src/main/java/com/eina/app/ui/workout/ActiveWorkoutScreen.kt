package com.eina.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.ui.components.BottomTimerBar
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandSurface
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
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
    var restDialogFor by remember { mutableStateOf<SessionExerciseUi?>(null) }

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
            SessionHeader(
                elapsedSeconds = state.elapsedSeconds,
                volumeKg = state.volumeKg,
                completedSets = state.completedSets,
                totalSets = state.totalSets,
                progress = state.progress,
                onFinish = { viewModel.finishWorkout(onFinished) },
                modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Spacing.xl,
                    end = Spacing.xl,
                    top = Spacing.sm,
                    bottom = 220.dp
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
                        onEditRest = { restDialogFor = exercise },
                        onAddSet = { viewModel.addSet(exercise.workoutExerciseId) },
                        onRemoveSet = { setId -> viewModel.removeSet(exercise.workoutExerciseId, setId) },
                        onSetValuesChange = { setId, reps, weight ->
                            viewModel.updateSetValues(exercise.workoutExerciseId, setId, reps, weight)
                        },
                        onToggleSet = { setId, completed ->
                            if (completed) viewModel.uncompleteSet(exercise.workoutExerciseId, setId)
                            else viewModel.completeSet(exercise.workoutExerciseId, setId)
                        }
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

    restDialogFor?.let { exercise ->
        RestDialog(
            currentSeconds = exercise.restSeconds,
            onConfirm = { seconds ->
                viewModel.setRestSeconds(exercise.workoutExerciseId, seconds)
                restDialogFor = null
            },
            onDismiss = { restDialogFor = null }
        )
    }
}

/** Intestazione compatta: durata, volume, serie svolte e barra di avanzamento della sessione. */
@Composable
private fun SessionHeader(
    elapsedSeconds: Int,
    volumeKg: Double,
    completedSets: Int,
    totalSets: Int,
    progress: Float,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island

    IslandSurface(modifier = modifier.fillMaxWidth(), shape = IslandShape, elevation = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderMetric("Durata", formatDuration(elapsedSeconds), Modifier.weight(1f))
                HeaderMetric("Volume", formatVolume(volumeKg), Modifier.weight(1f))
                HeaderMetric("Serie", "$completedSets/$totalSets", Modifier.weight(1f))
                IslandButton(text = "Termina", onClick = onFinish)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(PillShape)
                    .background(island.sunken)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun HeaderMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = EinaTheme.island.textSecondary
        )
        Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
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
    onEditRest: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onSetValuesChange: (Long, Int?, Double?) -> Unit,
    onToggleSet: (Long, Boolean) -> Unit
) {
    val island = EinaTheme.island
    var menuOpen by remember { mutableStateOf(false) }

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IslandShape,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Titolo + un solo punto di accesso alle azioni: riordino ed eliminazione stanno nel menu,
        // cosi' la riga resta pulita come in una scheda di allenamento cartacea.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IslandIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "Azioni esercizio",
                    onClick = { menuOpen = true },
                    containerColor = island.sunken,
                    size = 34.dp
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (canMoveUp) {
                        DropdownMenuItem(text = { Text("Sposta su") }, onClick = { onMoveUp(); menuOpen = false })
                    }
                    if (canMoveDown) {
                        DropdownMenuItem(text = { Text("Sposta giu'") }, onClick = { onMoveDown(); menuOpen = false })
                    }
                    DropdownMenuItem(text = { Text("Tempo di recupero") }, onClick = { onEditRest(); menuOpen = false })
                    DropdownMenuItem(text = { Text("Rimuovi esercizio") }, onClick = { onRemove(); menuOpen = false })
                }
            }
        }

        // Chip recupero: modificabile durante l'allenamento, non solo in fase di routine.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .clip(PillShape)
                .background(island.sunken)
                .clickable(onClick = onEditRest)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Icon(
                Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Recupero ${formatDuration(exercise.restSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary
            )
        }

        SetTableHeader()

        exercise.sets.forEach { set ->
            SetRow(
                set = set,
                onValuesChange = { reps, weight -> onSetValuesChange(set.id, reps, weight) },
                onToggle = { onToggleSet(set.id, set.completedAt != null) },
                onRemove = { onRemoveSet(set.id) }
            )
        }

        Text(
            text = "+ Aggiungi serie",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(island.sunken)
                .clickable(onClick = onAddSet)
                .padding(vertical = Spacing.sm)
        )
    }
}

@Composable
private fun SetTableHeader() {
    val island = EinaTheme.island
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TableLabel("Serie", Modifier.width(38.dp))
        TableLabel("Precedente", Modifier.weight(1.2f))
        TableLabel("Kg", Modifier.weight(1f))
        TableLabel("Rip", Modifier.weight(1f))
        Box(Modifier.size(36.dp))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(island.outlineSubtle)
    )
}

@Composable
private fun TableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EinaTheme.island.textSecondary,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun SetRow(
    set: SessionSetUi,
    onValuesChange: (Int?, Double?) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    val island = EinaTheme.island
    val completed = set.completedAt != null
    var rowMenuOpen by remember { mutableStateOf(false) }

    // Il testo digitato vive nella UI, non nel modello: passando ogni tasto per Double
    // "52." diventerebbe "52.0" e il decimale successivo sarebbe impossibile da scrivere.
    var weightText by remember(set.id) { mutableStateOf(set.weight?.toString() ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.actualReps?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(modifier = Modifier.width(38.dp), contentAlignment = Alignment.Center) {
            if (set.isPR) {
                Text(
                    text = "PR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, PillShape)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            } else {
                Text(
                    "${set.setIndex + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = island.textSecondary,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { rowMenuOpen = true }
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                )
            }
            DropdownMenu(expanded = rowMenuOpen, onDismissRequest = { rowMenuOpen = false }) {
                DropdownMenuItem(text = { Text("Elimina serie") }, onClick = { onRemove(); rowMenuOpen = false })
            }
        }

        Text(
            text = set.previous?.let { formatPrevious(it) } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.2f)
        )

        SetValueField(
            value = weightText,
            placeholder = set.targetWeight?.let { formatNumber(it) } ?: set.previous?.weight?.let { formatNumber(it) },
            onValueChange = { text ->
                weightText = text
                onValuesChange(repsText.toIntOrNull(), text.toDoubleOrNull())
            },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )

        SetValueField(
            value = repsText,
            placeholder = set.targetReps?.toString() ?: set.previous?.actualReps?.toString(),
            onValueChange = { text ->
                repsText = text
                onValuesChange(text.toIntOrNull(), weightText.toDoubleOrNull())
            },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )

        IslandIconButton(
            icon = Icons.Outlined.Check,
            contentDescription = if (completed) "Annulla serie" else "Completa serie",
            onClick = onToggle,
            containerColor = if (completed) MaterialTheme.colorScheme.primary else island.sunken,
            contentColor = if (completed) Color.White else island.textSecondary,
            size = 36.dp
        )
    }
}

/**
 * Campo numerico della tabella serie: nessuna etichetta, segnaposto grigio col valore target o
 * dell'ultima volta (che resta un suggerimento, non un dato registrato).
 */
@Composable
private fun SetValueField(
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
        textStyle = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(PillShape)
            .background(island.sunken)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder ?: "–",
                        style = MaterialTheme.typography.titleSmall,
                        color = island.textSecondary
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun RestDialog(
    currentSeconds: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var seconds by remember { mutableStateOf(currentSeconds) }
    val presets = listOf(30, 60, 90, 120, 180)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = IslandShape,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Tempo di recupero", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(formatDuration(seconds), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    presets.forEach { preset ->
                        com.eina.app.ui.components.IslandChip(
                            text = formatDuration(preset),
                            selected = seconds == preset,
                            onClick = { seconds = preset }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    IslandSecondaryButton(text = "-15s", onClick = { seconds = (seconds - 15).coerceAtLeast(0) }, modifier = Modifier.weight(1f))
                    IslandSecondaryButton(text = "+15s", onClick = { seconds += 15 }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(seconds) }) { Text("Applica") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
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

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

private fun formatVolume(kg: Double): String =
    if (kg >= 1000) "%.1ft".format(kg / 1000) else "%.0f kg".format(kg)

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun formatPrevious(set: com.eina.app.data.db.SetEntryEntity): String {
    val weight = set.weight?.let { "${formatNumber(it)}kg" }
    val reps = set.actualReps?.let { "$it" }
    return listOfNotNull(weight, reps).joinToString(" × ").ifBlank { "—" }
}
