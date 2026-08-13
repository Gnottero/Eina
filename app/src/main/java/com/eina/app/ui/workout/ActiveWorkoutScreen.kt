package com.eina.app.ui.workout

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eina.app.R
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.usesDecimalField
import com.eina.app.data.db.usesDistance
import com.eina.app.data.db.usesWeight
import com.eina.app.ui.components.BottomTimerBar
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.ExercisePickerSheet
import com.eina.app.ui.components.SetTableHeader
import com.eina.app.ui.components.SetValueField
import com.eina.app.ui.components.SwipeToDeleteSetRow
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.previousColumnWeight
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ReorderRow
import com.eina.app.ui.components.ReorderSheet
import com.eina.app.ui.components.IslandSurface
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.RestTimeSheet
import com.eina.app.ui.components.SetTypeIndicator
import com.eina.app.ui.components.SetTypeSheet
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.StopwatchController
import com.eina.app.ui.components.StopwatchIconButton
import com.eina.app.ui.components.StopwatchSheet
import com.eina.app.ui.components.SupersetBadge
import com.eina.app.ui.components.SupersetOption
import com.eina.app.ui.components.SupersetSheet
import com.eina.app.ui.components.supersetColor
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.domain.RoutineChange
import com.eina.app.domain.Superset
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.library.localized
import com.eina.app.ui.routine.launchPlaylist
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import com.eina.app.ui.theme.label
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * La schermata dell'allenamento, in due vesti.
 *
 * Con `editing` a true la stessa schermata corregge un allenamento gia' nello storico: le serie,
 * gli esercizi, i superset e le note si toccano con gli stessi gesti, perche' un allenamento
 * passato e' fatto della stessa materia di uno in corso. Cambia il contorno — niente cronometro
 * che scorre, niente recupero che parte, niente "Annulla" — e in fondo il salvataggio rifa' i
 * record su tutto lo storico.
 */
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    onFinished: () -> Unit,
    onExit: () -> Unit = {},
    onCancelled: () -> Unit = {},
    onOpenExercise: (Long) -> Unit = {},
    editing: Boolean = false,
    viewModel: ActiveWorkoutViewModel = koinViewModel(parameters = { parametersOf(sessionId, editing) })
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var restSheetFor by remember { mutableStateOf<Long?>(null) }
    var actionsSheetFor by remember { mutableStateOf<Long?>(null) }
    var notesSheetFor by remember { mutableStateOf<Long?>(null) }
    var setActionsFor by remember { mutableStateOf<SetRef?>(null) }
    var setTypeFor by remember { mutableStateOf<SetRef?>(null) }
    var supersetSheetFor by remember { mutableStateOf<Long?>(null) }
    var replaceSheetFor by remember { mutableStateOf<Long?>(null) }
    var showReorder by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    // Modifiche rispetto alla scheda di partenza: se ce ne sono, fra il "Termina" e il riepilogo
    // si passa dalla domanda su cosa farne.
    var routineChanges by remember { mutableStateOf<List<RoutineChange>>(emptyList()) }
    var confirmCancel by remember { mutableStateOf(false) }

    // I fogli sono ancorati all'id, non alla copia dell'esercizio: cosi' restano aperti sui dati
    // aggiornati anche se nel frattempo cambia una serie.
    val restSheetExercise = state.exercises.find { it.workoutExerciseId == restSheetFor }
    val actionsSheetExercise = state.exercises.find { it.workoutExerciseId == actionsSheetFor }
    val notesSheetExercise = state.exercises.find { it.workoutExerciseId == notesSheetFor }
    val supersetSheetExercise = state.exercises.find { it.workoutExerciseId == supersetSheetFor }
    val replaceSheetExercise = state.exercises.find { it.workoutExerciseId == replaceSheetFor }

    // Lettera del superset: assegnata dall'ordine in cui i giri compaiono nella lista, cosi' il
    // primo superset dall'alto e' sempre A.
    val supersetLetters = Superset.letters(state.exercises.map { it.supersetGroup })

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
                playlistUri = state.playlistUri,
                playlistType = state.playlistType,
                editing = editing,
                startTime = state.startTime,
                onFinish = { confirmFinish = true },
                onCancel = { confirmCancel = true },
                // Uscendo dalla correzione i record si rifanno lo stesso: le modifiche sono gia'
                // scritte serie per serie, e lasciarle senza ricalcolo darebbe record sbagliati.
                onExit = {
                    if (!editing) onExit()
                    // Tolte tutte le serie svolte l'allenamento non esiste piu': si esce come da
                    // "Annulla", perche' il riepilogo alle spalle non ha piu' niente da mostrare.
                    else viewModel.saveEdits(state.startTime, state.elapsedSeconds) { kept ->
                        if (kept) onExit() else onCancelled()
                    }
                },
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
                            title = stringResource(R.string.active_empty_title),
                            description = stringResource(R.string.active_empty_description)
                        )
                    }
                }

                items(state.exercises, key = { it.workoutExerciseId }) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onOpenActions = { actionsSheetFor = exercise.workoutExerciseId },
                        onOpenExercise = { onOpenExercise(exercise.exerciseId) },
                        onOpenSetActions = { setId ->
                            setActionsFor = SetRef(exercise.workoutExerciseId, setId)
                        },
                        onOpenSetType = { setId ->
                            setTypeFor = SetRef(exercise.workoutExerciseId, setId)
                        },
                        onEditRest = { restSheetFor = exercise.workoutExerciseId },
                        supersetLetter = exercise.supersetGroup?.let { supersetLetters[it] },
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
                        text = stringResource(R.string.action_add_exercise),
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
        ExerciseActionsSheet(
            exercise = actionsSheetExercise,
            // Con un esercizio solo (o un superset solo) non c'e' niente da riordinare.
            canReorder = supersetBlocks(state.exercises, supersetLetters).size > 1,
            onOpenExercise = {
                onOpenExercise(actionsSheetExercise.exerciseId)
                actionsSheetFor = null
            },
            onEditNotes = {
                notesSheetFor = actionsSheetExercise.workoutExerciseId
                actionsSheetFor = null
            },
            onReorder = { showReorder = true; actionsSheetFor = null },
            onEditRest = {
                restSheetFor = actionsSheetExercise.workoutExerciseId
                actionsSheetFor = null
            },
            onEditSuperset = {
                supersetSheetFor = actionsSheetExercise.workoutExerciseId
                actionsSheetFor = null
            },
            onReplace = {
                replaceSheetFor = actionsSheetExercise.workoutExerciseId
                actionsSheetFor = null
            },
            supersetLetter = actionsSheetExercise.supersetGroup?.let { supersetLetters[it] },
            onAddSet = { viewModel.addSet(actionsSheetExercise.workoutExerciseId); actionsSheetFor = null },
            onRemove = { viewModel.removeExercise(actionsSheetExercise.workoutExerciseId); actionsSheetFor = null },
            onDismiss = { actionsSheetFor = null }
        )
    }

    if (showReorder) {
        // Si trascinano blocchi, non card: un superset e' una sequenza e si sposta intero.
        ReorderSheet(
            title = stringResource(R.string.reorder_title),
            rows = supersetBlocks(state.exercises, supersetLetters),
            onConfirm = { keys -> viewModel.applyOrder(keys.flatMap { it.split(',').map(String::toLong) }) },
            onDismiss = { showReorder = false }
        )
    }

    if (replaceSheetExercise != null) {
        // La voce resta la stessa: cambia solo il movimento, e con esso non si perde il posto
        // nel superset.
        ExercisePickerSheet(
            exercises = state.availableExercises,
            title = stringResource(R.string.action_replace_exercise),
            onPick = { picked ->
                viewModel.replaceExercise(replaceSheetExercise.workoutExerciseId, picked)
                replaceSheetFor = null
            },
            onDismiss = { replaceSheetFor = null }
        )
    }

    if (restSheetExercise != null) {
        RestTimeSheet(
            currentSeconds = restSheetExercise.restSeconds,
            onConfirm = { seconds -> viewModel.setRestSeconds(restSheetExercise.workoutExerciseId, seconds) },
            onDismiss = { restSheetFor = null },
            description = if (restSheetExercise.supersetGroup != null) {
                stringResource(R.string.superset_rest_description)
            } else {
                stringResource(R.string.active_rest_apply_description)
            }
        )
    }

    if (notesSheetExercise != null) {
        ExerciseNotesSheet(
            exercise = notesSheetExercise,
            onSave = { notes -> viewModel.setExerciseNotes(notesSheetExercise.workoutExerciseId, notes) },
            onDismiss = { notesSheetFor = null }
        )
    }

    if (supersetSheetExercise != null) {
        SupersetSheet(
            exerciseName = supersetSheetExercise.name.localized(),
            current = supersetSheetExercise.supersetGroup,
            options = state.exercises
                .filter { it.supersetGroup != null }
                .groupBy { it.supersetGroup!! }
                .map { (group, members) ->
                    SupersetOption(
                        group = group,
                        letter = supersetLetters[group].orEmpty(),
                        members = members.map { it.name.localized() }
                    )
                }
                .sortedBy { it.letter },
            onSelect = { group -> viewModel.setSupersetGroup(supersetSheetExercise.workoutExerciseId, group) },
            onNewGroup = {
                viewModel.setSupersetGroup(supersetSheetExercise.workoutExerciseId, viewModel.nextSupersetGroup())
            },
            onDismiss = { supersetSheetFor = null }
        )
    }

    setTypeFor?.let { ref ->
        val current = state.exercises
            .find { it.workoutExerciseId == ref.workoutExerciseId }
            ?.sets?.find { it.id == ref.setId }
        if (current == null) {
            setTypeFor = null
        } else {
            SetTypeSheet(
                current = current.setType,
                onSelect = { type -> viewModel.setSetType(ref.workoutExerciseId, ref.setId, type) },
                onDismiss = { setTypeFor = null }
            )
        }
    }

    setActionsFor?.let { ref ->
        // Titolo col numero che la serie porta in tabella: le warmup non hanno numero, quindi
        // per loro resta il titolo generico.
        val sets = state.exercises.find { it.workoutExerciseId == ref.workoutExerciseId }?.sets.orEmpty()
        val target = sets.find { it.id == ref.setId }
        val number = sets
            .takeWhile { it.id != ref.setId }
            .count { it.setType.countsAsWorking } + 1
        IslandBottomSheet(
            onDismiss = { setActionsFor = null },
            title = if (target != null && target.setType.countsAsWorking) {
                stringResource(R.string.active_set_sheet_title, number)
            } else {
                stringResource(R.string.active_set_sheet_title_generic)
            }
        ) {
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.active_delete_set),
                destructive = true,
                onClick = {
                    viewModel.removeSet(ref.workoutExerciseId, ref.setId)
                    setActionsFor = null
                }
            )
        }
    }

    if (confirmFinish) {
        // "Termina" e' l'unico modo di chiudere una sessione: si conferma perche' e' irreversibile,
        // e nella conferma si correggono data e durata prima di scriverle nello storico. Sulla
        // correzione di un allenamento passato lo stesso foglio serve a spostarne data e durata.
        FinishWorkoutSheet(
            startTime = state.startTime,
            elapsedSeconds = state.elapsedSeconds,
            // "Non finira' nello storico" vale per la sessione senza serie svolte, non solo per
            // quella senza esercizi: e' quella che viene eliminata al posto di essere salvata.
            isEmpty = state.completedSets == 0 && !editing,
            editing = editing,
            onConfirm = { startTime, duration ->
                if (editing) {
                    viewModel.saveEdits(startTime, duration) { kept ->
                        if (kept) onFinished() else onCancelled()
                    }
                    return@FinishWorkoutSheet
                }
                // Senza esercizi la sessione viene eliminata invece che salvata: si esce come da
                // "Annulla", perche' non c'e' nessun riepilogo da mostrare.
                viewModel.finishWorkout(
                    startTime = startTime,
                    durationSeconds = duration,
                    onNeedsRoutineAnswer = { changes -> routineChanges = changes },
                    onFinished = { saved -> if (saved) onFinished() else onCancelled() }
                )
            },
            onDismiss = { confirmFinish = false }
        )
    }

    if (routineChanges.isNotEmpty()) {
        // La domanda arriva prima che la sessione si chiuda: cosi' vale anche per un allenamento
        // senza serie svolte, che nello storico non ci finisce ma la scheda l'ha comunque
        // cambiata (esercizi tolti, serie aggiunte, recuperi diversi).
        val answer = { update: Boolean ->
            routineChanges = emptyList()
            viewModel.answerRoutineSync(update) { saved -> if (saved) onFinished() else onCancelled() }
        }
        RoutineSyncSheet(
            routineName = state.routineName,
            changes = routineChanges,
            onUpdate = { answer(true) },
            onKeep = { answer(false) }
        )
    }

    if (confirmCancel) {
        // Annullare butta via la sessione: si conferma perche' le serie gia' segnate spariscono.
        IslandAlertDialog(
            title = stringResource(R.string.active_cancel_confirm_title),
            text = stringResource(R.string.active_cancel_confirm_text),
            confirmLabel = stringResource(R.string.active_cancel_confirm_action),
            onConfirm = {
                confirmCancel = false
                viewModel.cancelWorkout(onCancelled)
            },
            dismissLabel = stringResource(R.string.action_continue),
            onDismiss = { confirmCancel = false }
        )
    }
}

/** Riferimento a una serie dentro la sessione: chiave del foglio azioni aperto col tocco lungo. */
private data class SetRef(val workoutExerciseId: Long, val setId: Long)

/**
 * Intestazione di sessione: durata e volume come due metriche grandi, avanzamento affidato alla
 * sola barra (il contatore numerico delle serie sarebbe ridondante) e chiusura dell'allenamento.
 */
@Composable
private fun SessionHeader(
    elapsedSeconds: Int,
    volumeKg: Double,
    progress: Float,
    playlistUri: String?,
    playlistType: PlaylistType?,
    editing: Boolean,
    startTime: Long,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    val stopwatch: StopwatchController = koinInject()
    val stopwatchState by stopwatch.state.collectAsState()
    var showStopwatch by remember { mutableStateOf(false) }
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
                    contentDescription = stringResource(R.string.active_exit_cd),
                    onClick = onExit,
                    containerColor = island.sunken,
                    size = 40.dp
                )
                Text(
                    // Correggendo un allenamento passato il titolo dice la sua data: e' l'unico
                    // modo di sapere quale si sta riscrivendo.
                    text = if (editing) formatFullDate(startTime) else stringResource(R.string.active_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = island.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // La playlist si lancia da qui, dove serve davvero: nell'editor della routine
                // si sta scrivendo una scheda, non ci si sta allenando.
                if (playlistType != null && !playlistUri.isNullOrBlank()) {
                    IslandIconButton(
                        icon = Icons.Outlined.MusicNote,
                        contentDescription = stringResource(R.string.active_play_playlist_cd),
                        onClick = {
                            // Senza app musicale ne' browser non succede nulla: lo si dice,
                            // invece di lasciare il tasto muto.
                            if (!launchPlaylist(context, playlistUri, playlistType)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.active_playlist_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        containerColor = island.sunken,
                        size = 40.dp
                    )
                }
                // Cronometro a portata di mano anche in palestra: e' lo stesso della Dashboard,
                // quindi un conteggio avviato prima continua qui. Sta dopo la playlist, che
                // tiene il suo posto storico in fondo alla riga.
                StopwatchIconButton(
                    running = stopwatchState.running,
                    onClick = { showStopwatch = true },
                    containerColor = island.sunken,
                    size = 40.dp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                MetricTile(
                    icon = Icons.Outlined.Timer,
                    label = stringResource(R.string.stat_duration),
                    value = formatDuration(elapsedSeconds),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.FitnessCenter,
                    label = stringResource(R.string.stat_volume),
                    value = formatVolumeValue(volumeKg),
                    unit = stringResource(R.string.unit_kg),
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
                // Sulla correzione il tasto apre lo stesso foglio, ma quel che conferma sono data
                // e durata di un allenamento che nello storico c'e' gia'.
                text = stringResource(if (editing) R.string.edit_session_save else R.string.active_finish),
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )

            // "Annulla" butta via la sessione: su un allenamento gia' registrato non ha senso,
            // si elimina dallo storico dove si e' scelto di tenerlo.
            if (!editing) {
                Text(
                    text = stringResource(R.string.active_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = DestructiveRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .clickable { onCancel() }
                        .padding(vertical = Spacing.sm)
                )
            }
        }
    }

    if (showStopwatch) {
        StopwatchSheet(controller = stopwatch, onDismiss = { showStopwatch = false })
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
                    // Stesso colore del numero: l'unita' ne fa parte.
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseCard(
    exercise: SessionExerciseUi,
    onOpenActions: () -> Unit,
    onOpenExercise: () -> Unit,
    onOpenSetActions: (Long) -> Unit,
    onOpenSetType: (Long) -> Unit,
    onEditRest: () -> Unit,
    supersetLetter: String?,
    onAddSet: () -> Unit,
    onRemoveSet: (Long) -> Unit,
    onSetValuesChange: (Long, Int?, Double?) -> Unit,
    onToggleSet: (Long, Boolean) -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val supersetTint = supersetLetter?.let { supersetColor(it) }

    IslandCard(
        // Il contorno colorato dice a colpo d'occhio quali card fanno parte dello stesso giro.
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (supersetTint == null) Modifier
                else Modifier.border(2.dp, supersetTint, IslandShape)
            ),
        shape = IslandShape,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        // Niente tre puntini: le azioni si aprono col tocco lungo sulla card.
        onLongClick = onOpenActions
    ) {
        if (supersetLetter != null) {
            SupersetBadge(letter = supersetLetter)
        }

        // Il nome porta alla scheda dell'esercizio: durante una serie serve rileggere
        // l'esecuzione, non ricercarlo in libreria.
        Text(
            exercise.name.localized(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                // Raggio piccolo: TileShape (24dp) e' piu' alto di mezza riga di testo e la sua
                // curva mangiava le prime e le ultime lettere dei nomi lunghi. Cosi' il nome
                // resta a filo del bordo della card, in colonna con recupero e tabella serie.
                .clip(RoundedCornerShape(8.dp))
                // Anche i figli cliccabili devono rispondere al tocco lungo: senza, il gesto
                // funzionerebbe solo sui pochi punti morti della card.
                .combinedClickable(
                    onLongClick = { hapticTap(); onOpenActions() },
                    onClick = { hapticTap(); onOpenExercise() }
                )
                .padding(vertical = Spacing.xs)
        )

        exercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }

        // Chip recupero: modificabile durante l'allenamento, non solo in fase di routine.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .clip(PillShape)
                .background(island.sunken)
                .combinedClickable(
                    onLongClick = { hapticTap(); onOpenActions() },
                    onClick = { hapticTap(); onEditRest() }
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
                text = stringResource(R.string.rest_label, formatDuration(exercise.restSeconds)),
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SetTableHeader(weightType = exercise.weightType)
            // Il numero conta solo le serie di lavoro: un riscaldamento in mezzo porta la W, non
            // ruba il numero alla serie dopo.
            var workingNumber = 0
            exercise.sets.forEach { set ->
                if (set.setType.countsAsWorking) workingNumber++
                // Trascinando la riga a sinistra la serie sparisce: col tocco lungo era l'unica
                // via, e fra i campi numerici restava poco da toccare.
                SwipeToDeleteSetRow(onDelete = { onRemoveSet(set.id) }) {
                    SetRow(
                        set = set,
                        number = workingNumber,
                        weightType = exercise.weightType,
                        onTypeClick = { onOpenSetType(set.id) },
                        onValuesChange = { reps, weight -> onSetValuesChange(set.id, reps, weight) },
                        onToggle = { onToggleSet(set.id, set.completedAt != null) },
                        onLongClick = { onOpenSetActions(set.id) }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.active_add_set),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(PillShape)
                .background(island.sunken)
                .combinedClickable(
                    onLongClick = { hapticTap(); onOpenActions() },
                    onClick = { hapticTap(); onAddSet() }
                )
                .padding(vertical = Spacing.md)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRow(
    set: SessionSetUi,
    number: Int,
    weightType: WeightType,
    onValuesChange: (Int?, Double?) -> Unit,
    onToggle: () -> Unit,
    onTypeClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val completed = set.completedAt != null

    // Il testo digitato vive nella UI, non nel modello: passando ogni tasto per Double
    // "52." diventerebbe "52.0" e il decimale successivo sarebbe impossibile da scrivere.
    var weightText by remember(set.id) { mutableStateOf(set.weight?.toString() ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.actualReps?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            // Tocco lungo sulla riga = azioni della serie. I campi numerici si prendono i tocchi
            // che li riguardano, il resto della riga resta area utile per il gesto.
            .combinedClickable(onLongClick = { hapticTap(); onLongClick() }, onClick = {})
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Il segno della serie e' anche il comando: un tocco apre la scelta W / numero / F / D.
        SetTypeIndicator(
            type = set.setType,
            number = number,
            isPR = set.isPR,
            onClick = onTypeClick,
            modifier = Modifier.width(40.dp)
        )

        Text(
            text = set.previous?.let { formatPrevious(it, weightType) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(previousColumnWeight(weightType))
        )

        // Segnaposto = valori proposti dal ViewModel, gli stessi che vengono registrati se la
        // serie viene chiusa senza digitare nulla.
        // Stesso campo decimale per i kg e per i chilometri: cambia l'etichetta in testa alla
        // colonna, non la casella.
        if (weightType.usesDecimalField) {
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
        }

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
                contentDescription = stringResource(R.string.active_undo_set_cd),
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Azioni sull'esercizio in corso: righe grandi con icona, al posto del menu a tendina minuscolo. */
@Composable
private fun ExerciseActionsSheet(
    exercise: SessionExerciseUi,
    canReorder: Boolean,
    onOpenExercise: () -> Unit,
    onEditNotes: () -> Unit,
    onReorder: () -> Unit,
    onEditRest: () -> Unit,
    onEditSuperset: () -> Unit,
    onReplace: () -> Unit,
    supersetLetter: String?,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    IslandBottomSheet(onDismiss = onDismiss, title = exercise.name.localized()) {
        SheetActionRow(
            icon = Icons.Outlined.Search,
            label = stringResource(R.string.active_open_exercise),
            onClick = onOpenExercise
        )
        SheetActionRow(
            icon = Icons.AutoMirrored.Outlined.Notes,
            label = stringResource(if (exercise.notes.isNullOrBlank()) R.string.note_add else R.string.note_edit),
            description = exercise.notes?.takeIf { it.isNotBlank() },
            onClick = onEditNotes
        )
        SheetActionRow(
            icon = Icons.Outlined.Timer,
            label = stringResource(R.string.rest_time_title),
            description = formatDuration(exercise.restSeconds),
            onClick = onEditRest
        )
        SheetActionRow(
            icon = Icons.Outlined.Repeat,
            label = stringResource(R.string.superset_action),
            description = supersetLetter?.let { stringResource(R.string.superset_badge, it) }
                ?: stringResource(R.string.superset_action_none),
            onClick = onEditSuperset
        )
        SheetActionRow(
            icon = Icons.Outlined.SwapHoriz,
            label = stringResource(R.string.action_replace_exercise),
            description = stringResource(R.string.active_replace_exercise_description),
            onClick = onReplace
        )
        SheetActionRow(
            icon = Icons.Outlined.Add,
            label = stringResource(R.string.active_add_set_action),
            onClick = onAddSet
        )
        if (canReorder) {
            SheetActionRow(
                icon = Icons.Outlined.SwapVert,
                label = stringResource(R.string.action_reorder),
                description = stringResource(R.string.reorder_action_description),
                onClick = onReorder
            )
        }
        SheetActionRow(
            icon = Icons.Outlined.Delete,
            label = stringResource(R.string.action_remove_exercise),
            description = stringResource(R.string.active_remove_exercise_description),
            destructive = true,
            onClick = onRemove
        )
    }
}

/**
 * Nota dell'esercizio durante l'allenamento: carico usato, sensazioni, correzioni di tecnica.
 * Resta nella sessione e non riscrive la nota della routine.
 */
@Composable
private fun ExerciseNotesSheet(
    exercise: SessionExerciseUi,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(exercise.workoutExerciseId) { mutableStateOf(exercise.notes.orEmpty()) }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.note_sheet_title, exercise.name.localized())) {
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

/**
 * "60kg×8" dove il carico esiste, le sole ripetizioni (o i secondi) a corpo libero e a tempo:
 * stampare "0kg×8" su una trazione sarebbe un dato inventato.
 */
private fun formatPrevious(set: com.eina.app.data.db.SetEntryEntity, weightType: WeightType): String {
    val reps = set.actualReps?.toString()
    // Sulla distanza il "precedente" sono i chilometri col tempo, non un carico per ripetizioni.
    // Forma compatta come "5,2km·30": per esteso la colonna e' troppo stretta e taglia il tempo.
    if (weightType.usesDistance) {
        val km = set.weight?.let { "${formatDecimal(it)}km" }
        return listOfNotNull(km, reps).joinToString("·").ifBlank { "—" }
    }
    if (!weightType.usesWeight) return reps ?: "—"
    val weight = set.weight?.let { "${formatNumber(it)}kg" }
    return listOfNotNull(weight, reps).joinToString("×").ifBlank { "—" }
}

/**
 * Gli esercizi della sessione visti come blocchi trascinabili: un superset e' una riga sola,
 * perche' i suoi membri si spostano insieme. La chiave della riga sono gli id dei membri, cosi'
 * l'ordine confermato si riappiattisce senza tenere una mappa a parte.
 */
@Composable
private fun supersetBlocks(
    exercises: List<SessionExerciseUi>,
    letters: Map<Int, String>
): List<ReorderRow> {
    // getString e locale letti fuori: dentro le lambda di map non si chiamano composable.
    val context = LocalContext.current
    val locale = currentLocale()
    return Superset.blocksOf(exercises.map { Superset.Member(it.workoutExerciseId, it.supersetGroup) })
        .map { block ->
            val members = block.mapNotNull { member ->
                exercises.find { it.workoutExerciseId == member.id }
            }
            val group = members.firstOrNull()?.supersetGroup
            val letter = group?.let { letters[it] }
            ReorderRow(
                key = members.joinToString(",") { it.workoutExerciseId.toString() },
                title = if (letter == null) {
                    members.firstOrNull()?.name?.localized(locale).orEmpty()
                } else {
                    context.getString(R.string.superset_badge, letter)
                },
                subtitle = if (letter == null) null else members.joinToString(" · ") { it.name.localized(locale) },
                tint = letter?.let { supersetColor(it) }
            )
        }
}
