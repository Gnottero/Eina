package com.eina.app.ui.workout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
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
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandSurface
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MuscleGroupCategory
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import com.eina.app.ui.theme.primaryCategoryFor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    onExit: () -> Unit = {},
    viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(sessionId) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var restSheetFor by remember { mutableStateOf<Long?>(null) }
    var actionsSheetFor by remember { mutableStateOf<Long?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }

    // I fogli sono ancorati all'id, non alla copia dell'esercizio: cosi' restano aperti sui dati
    // aggiornati anche se nel frattempo cambia una serie.
    val restSheetExercise = state.exercises.find { it.workoutExerciseId == restSheetFor }
    val actionsSheetExercise = state.exercises.find { it.workoutExerciseId == actionsSheetFor }

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
                progress = state.progress,
                onFinish = { confirmFinish = true },
                onExit = onExit,
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

                items(state.exercises, key = { it.workoutExerciseId }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onOpenActions = { actionsSheetFor = exercise.workoutExerciseId },
                        onEditRest = { restSheetFor = exercise.workoutExerciseId },
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
        ExercisePickerSheet(
            exercises = state.availableExercises,
            onPick = {
                viewModel.addExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    if (actionsSheetExercise != null) {
        val index = state.exercises.indexOf(actionsSheetExercise)
        ExerciseActionsSheet(
            exercise = actionsSheetExercise,
            canMoveUp = index > 0,
            canMoveDown = index < state.exercises.size - 1,
            onMoveUp = { viewModel.moveExercise(actionsSheetExercise.workoutExerciseId, -1); actionsSheetFor = null },
            onMoveDown = { viewModel.moveExercise(actionsSheetExercise.workoutExerciseId, 1); actionsSheetFor = null },
            onEditRest = {
                restSheetFor = actionsSheetExercise.workoutExerciseId
                actionsSheetFor = null
            },
            onAddSet = { viewModel.addSet(actionsSheetExercise.workoutExerciseId); actionsSheetFor = null },
            onRemove = { viewModel.removeExercise(actionsSheetExercise.workoutExerciseId); actionsSheetFor = null },
            onDismiss = { actionsSheetFor = null }
        )
    }

    if (restSheetExercise != null) {
        RestTimeSheet(
            currentSeconds = restSheetExercise.restSeconds,
            onChange = { seconds -> viewModel.setRestSeconds(restSheetExercise.workoutExerciseId, seconds) },
            onDismiss = { restSheetFor = null }
        )
    }

    if (confirmFinish) {
        // "Termina" e' l'unico modo di chiudere una sessione: si conferma perche' e' irreversibile.
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            shape = IslandShape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Terminare l'allenamento?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("La sessione verra' chiusa e salvata nello storico.") },
            confirmButton = {
                HapticTextButton(text = "Termina", onClick = {
                    confirmFinish = false
                    viewModel.finishWorkout(onFinished)
                })
            },
            dismissButton = {
                HapticTextButton(text = "Continua", onClick = { confirmFinish = false })
            }
        )
    }

}

/**
 * Intestazione di sessione: durata e volume come due metriche grandi, avanzamento affidato alla
 * sola barra (il contatore numerico delle serie sarebbe ridondante) e chiusura dell'allenamento.
 */
@Composable
private fun SessionHeader(
    elapsedSeconds: Int,
    volumeKg: Double,
    progress: Float,
    onFinish: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    // La barra si muove verso il nuovo valore invece di saltarci: il progresso cambia a scatti
    // di una serie alla volta e uno scatto secco su una barra sottile si legge male.
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "sessionProgress"
    )

    IslandSurface(modifier = modifier.fillMaxWidth(), shape = IslandShape, elevation = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Uscire mette l'allenamento in pausa "sociale": resta in corso, si rientra da Allena.
                IslandIconButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Esci senza terminare",
                    onClick = onExit,
                    containerColor = island.sunken,
                    size = 40.dp
                )
                Text(
                    text = "Allenamento in corso",
                    style = MaterialTheme.typography.labelLarge,
                    color = island.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                MetricTile(
                    icon = Icons.Outlined.Timer,
                    label = "Durata",
                    value = formatDuration(elapsedSeconds),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.FitnessCenter,
                    label = "Volume",
                    value = formatVolumeValue(volumeKg),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape)
                    .background(island.sunken)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            IslandButton(
                text = "Termina allenamento",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val island = EinaTheme.island
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(island.sunken)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary,
                maxLines = 1
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: SessionExerciseUi,
    onOpenActions: () -> Unit,
    onEditRest: () -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onSetValuesChange: (Long, Int?, Double?) -> Unit,
    onToggleSet: (Long, Boolean) -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IslandShape,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        // Titolo + un solo punto di accesso alle azioni: riordino ed eliminazione stanno nel
        // foglio, cosi' la riga resta pulita come in una scheda di allenamento cartacea.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IslandIconButton(
                icon = Icons.Outlined.MoreVert,
                contentDescription = "Azioni esercizio",
                onClick = onOpenActions,
                containerColor = island.sunken,
                size = 40.dp
            )
        }

        // Chip recupero: modificabile durante l'allenamento, non solo in fase di routine.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .clip(PillShape)
                .background(island.sunken)
                .clickable { hapticTap(); onEditRest() }
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
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

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SetTableHeader()
            exercise.sets.forEach { set ->
                SetRow(
                    set = set,
                    onValuesChange = { reps, weight -> onSetValuesChange(set.id, reps, weight) },
                    onToggle = { onToggleSet(set.id, set.completedAt != null) },
                    onRemove = { onRemoveSet(set.id) }
                )
            }
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
                .clickable { hapticTap(); onAddSet() }
                .padding(vertical = Spacing.md)
        )
    }
}

@Composable
private fun SetTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        TableLabel("Serie", Modifier.width(40.dp))
        TableLabel("Prec.", Modifier.weight(1.1f))
        TableLabel("Kg", Modifier.weight(1f))
        TableLabel("Rip", Modifier.weight(1f))
        Box(Modifier.size(42.dp))
    }
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
    val hapticTap = LocalHapticTap.current
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
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
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
                    style = MaterialTheme.typography.titleMedium,
                    color = island.textSecondary,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { hapticTap(); rowMenuOpen = true }
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                )
            }
            DropdownMenu(expanded = rowMenuOpen, onDismissRequest = { rowMenuOpen = false }) {
                HapticMenuItem("Elimina serie") { onRemove(); rowMenuOpen = false }
            }
        }

        Text(
            text = set.previous?.let { formatPrevious(it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.1f)
        )

        // Segnaposto = valori proposti dal ViewModel, gli stessi che vengono registrati se la
        // serie viene chiusa senza digitare nulla.
        SetValueField(
            value = weightText,
            placeholder = set.suggestedWeight?.let { formatNumber(it) },
            onValueChange = { typed ->
                weightText = sanitizeWeightInput(weightText, typed)
                onValuesChange(repsText.toIntOrNull(), weightText.toDoubleOrNull())
            },
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f)
        )

        SetValueField(
            value = repsText,
            placeholder = set.suggestedReps?.toString(),
            onValueChange = { typed ->
                repsText = typed.filter { it.isDigit() }.take(4)
                onValuesChange(repsText.toIntOrNull(), weightText.toDoubleOrNull())
            },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )

        SetCheckButton(completed = completed, onClick = onToggle)
    }
}

/**
 * Check della serie: cerchio vuoto finche' non e' svolta (un segno di spunta grigio si legge
 * come "gia' fatta"), pill piena viola quando e' completata.
 */
@Composable
private fun SetCheckButton(completed: Boolean, onClick: () -> Unit) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(PillShape)
            .background(if (completed) MaterialTheme.colorScheme.primary else Color.Transparent)
            .then(
                if (completed) Modifier
                else Modifier.border(1.5.dp, island.outlineSubtle, PillShape)
            )
            .clickable { hapticTap(); onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Annulla serie",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
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
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(PillShape)
            .background(island.sunken)
            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder ?: "–",
                        style = MaterialTheme.typography.titleMedium,
                        color = island.textSecondary
                    )
                }
                inner()
            }
        }
    )
}

/** Tetto al recupero impostabile: coerente col clamp del ViewModel (setRestSeconds). */
private const val MAX_REST_SECONDS = 600

/**
 * Recupero: foglio con conto in grande, due tasti tondi per la regolazione fine e una griglia di
 * durate. Ogni gesto si applica subito e la scelta di una durata chiude il foglio — non c'e' un
 * "Applica" da ricordare, come nel dialog di prima.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimeSheet(
    currentSeconds: Int,
    onChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    val presets = listOf(0, 30, 45, 60, 75, 90, 120, 150, 180, 240, 300)

    IslandBottomSheet(onDismiss = onDismiss, title = "Tempo di recupero") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            IslandIconButton(
                icon = Icons.Outlined.Remove,
                contentDescription = "-15 secondi",
                onClick = { onChange((currentSeconds - 15).coerceAtLeast(0)) },
                containerColor = island.sunken,
                size = 52.dp
            )
            Text(
                text = if (currentSeconds == 0) "Nessuno" else formatDuration(currentSeconds),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IslandIconButton(
                icon = Icons.Outlined.Add,
                contentDescription = "+15 secondi",
                onClick = { onChange((currentSeconds + 15).coerceAtMost(MAX_REST_SECONDS)) },
                containerColor = island.sunken,
                size = 52.dp
            )
        }

        Text(
            text = "Vale per tutte le serie non ancora svolte di questo esercizio.",
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary,
            modifier = Modifier.padding(vertical = Spacing.sm)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            presets.forEach { preset ->
                IslandChip(
                    text = if (preset == 0) "Nessuno" else formatDuration(preset),
                    selected = currentSeconds == preset,
                    onClick = { onChange(preset); onDismiss() }
                )
            }
        }
    }
}

/** Azioni sull'esercizio in corso: righe grandi con icona, al posto del menu a tendina minuscolo. */
@Composable
private fun ExerciseActionsSheet(
    exercise: SessionExerciseUi,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditRest: () -> Unit,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    IslandBottomSheet(onDismiss = onDismiss, title = exercise.name) {
        SheetActionRow(
            icon = Icons.Outlined.Timer,
            label = "Tempo di recupero",
            description = formatDuration(exercise.restSeconds),
            onClick = onEditRest
        )
        SheetActionRow(
            icon = Icons.Outlined.Add,
            label = "Aggiungi serie",
            onClick = onAddSet
        )
        if (canMoveUp) {
            SheetActionRow(
                icon = Icons.Outlined.KeyboardArrowUp,
                label = "Sposta su",
                onClick = onMoveUp
            )
        }
        if (canMoveDown) {
            SheetActionRow(
                icon = Icons.Outlined.KeyboardArrowDown,
                label = "Sposta giu'",
                onClick = onMoveDown
            )
        }
        SheetActionRow(
            icon = Icons.Outlined.Delete,
            label = "Rimuovi esercizio",
            description = "Elimina anche le serie registrate qui",
            destructive = true,
            onClick = onRemove
        )
    }
}

/** Azione testuale dei dialog, con lo stesso micro-feedback aptico dei controlli island. */
@Composable
private fun HapticTextButton(text: String, onClick: () -> Unit) {
    val hapticTap = LocalHapticTap.current
    TextButton(onClick = { hapticTap(); onClick() }) {
        Text(text, color = MaterialTheme.colorScheme.primary)
    }
}

/** Voce di menu con feedback aptico: il DropdownMenuItem Material non ne ha. */
@Composable
private fun HapticMenuItem(text: String, onClick: () -> Unit) {
    val hapticTap = LocalHapticTap.current
    DropdownMenuItem(text = { Text(text) }, onClick = { hapticTap(); onClick() })
}

/**
 * Scelta dell'esercizio da aggiungere alla sessione: ricerca per nome e filtro per gruppo
 * muscolare, come nella libreria. Con centinaia di esercizi in elenco, scorrere non basta.
 */
@Composable
private fun ExercisePickerSheet(
    exercises: List<ExerciseEntity>,
    onPick: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<MuscleGroupCategory?>(null) }

    val filtered = remember(exercises, query, category) {
        exercises.filter { exercise ->
            val matchesQuery = query.isBlank() || exercise.name.contains(query, ignoreCase = true)
            val matchesCategory = category == null ||
                primaryCategoryFor(exercise.muscleGroupsPrimary) == category
            matchesQuery && matchesCategory
        }
    }

    IslandBottomSheet(onDismiss = onDismiss, title = "Aggiungi esercizio") {
        IslandTextField(
            value = query,
            onValueChange = { query = it },
            label = "Cerca esercizio",
            leadingIcon = Icons.Outlined.Search,
            modifier = Modifier.fillMaxWidth()
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(vertical = Spacing.sm)
        ) {
            items(MuscleGroupCategory.entries) { entry ->
                IslandChip(
                    text = entry.label,
                    selected = category == entry,
                    accentColor = entry.color,
                    onClick = { category = if (category == entry) null else entry }
                )
            }
        }

        if (filtered.isEmpty()) {
            Text(
                text = if (exercises.isEmpty()) {
                    "Nessun esercizio in libreria."
                } else {
                    "Nessun esercizio trovato. Cambia filtro o cerca un altro nome."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                modifier = Modifier.padding(vertical = Spacing.lg)
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(380.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val exerciseCategory = primaryCategoryFor(exercise.muscleGroupsPrimary)
                    IslandCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(Spacing.md),
                        elevation = 0.dp,
                        color = island.sunken,
                        onClick = { onPick(exercise) }
                    ) {
                        Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = listOfNotNull(
                                exerciseCategory.label,
                                exercise.equipment?.takeIf { it.isNotBlank() }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = island.textSecondary
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

/** Volume come intero con separatore delle migliaia: l'unita' la stampa la tile a parte. */
private fun formatVolumeValue(kg: Double): String =
    "%,d".format(kg.toLong()).replace(',', '.')

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private fun formatPrevious(set: com.eina.app.data.db.SetEntryEntity): String {
    val weight = set.weight?.let { "${formatNumber(it)}kg" }
    val reps = set.actualReps?.let { "$it" }
    return listOfNotNull(weight, reps).joinToString("×").ifBlank { "—" }
}
