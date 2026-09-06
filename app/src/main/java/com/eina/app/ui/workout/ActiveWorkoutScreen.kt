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
import androidx.compose.runtime.key
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
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SkipNext
import com.eina.app.ui.components.ActivityRing
import com.eina.app.ui.components.RampBand
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.theme.MetricColors
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.usesDecimalField
import com.eina.app.data.db.usesDistance
import com.eina.app.data.db.usesWeight
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.ExercisePickerSheet
import com.eina.app.ui.components.PreviousValueText
import com.eina.app.ui.components.SetCheckSize
import com.eina.app.ui.components.SetColumnGap
import com.eina.app.ui.components.SetMarkerWidth
import com.eina.app.ui.components.SetRowInset
import com.eina.app.ui.components.SetTableHeader
import com.eina.app.ui.components.SetValueField
import com.eina.app.ui.components.SwipeToDeleteSetRow
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.previousColumnWeight
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.MetricTile
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
 * Workout screen, used both for the live session and, with [editing] true, for correcting a past
 * one: a past workout is made of the same data, so the gestures are shared. In editing mode there
 * is no running clock, no rest timer and no cancel action, and saving recomputes PR flags across
 * the whole history.
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
    // Differences against the source routine; when non-empty the user is asked whether to apply
    // them before the session is closed.
    var routineChanges by remember { mutableStateOf<List<RoutineChange>>(emptyList()) }
    var confirmCancel by remember { mutableStateOf(false) }
    var sessionActionsOpen by remember { mutableStateOf(false) }

    // Sheets are keyed by id and not by the exercise snapshot, so they keep showing fresh data
    // when a set changes while they are open.
    val restSheetExercise = state.exercises.find { it.workoutExerciseId == restSheetFor }
    val actionsSheetExercise = state.exercises.find { it.workoutExerciseId == actionsSheetFor }
    val notesSheetExercise = state.exercises.find { it.workoutExerciseId == notesSheetFor }
    val supersetSheetExercise = state.exercises.find { it.workoutExerciseId == supersetSheetFor }
    val replaceSheetExercise = state.exercises.find { it.workoutExerciseId == replaceSheetFor }

    // Superset letters follow the order the groups appear in the list: the topmost one is always A.
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
            // The same header every other screen carries: round back, small context line, large
            // title, one round action. The workout used to open with an island of its own that
            // repeated the app bar's job and pushed the first exercise below the fold.
            ScreenHeader(
                eyebrow = formatFullDate(state.startTime),
                title = stringResource(
                    if (editing) R.string.edit_session_sheet_title else R.string.active_title
                ),
                // Leaving the editor still saves: edits are written set by set, so skipping the
                // save would leave stale PR flags behind. If every completed set was removed the
                // session no longer exists and there is no summary to go back to.
                onBack = {
                    if (!editing) onExit()
                    else viewModel.saveEdits(state.startTime, state.elapsedSeconds) { kept ->
                        if (kept) onExit() else onCancelled()
                    }
                },
                trailing = {
                    IslandIconButton(
                        icon = Icons.Outlined.MoreHoriz,
                        contentDescription = stringResource(R.string.active_actions),
                        onClick = { sessionActionsOpen = true }
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = Spacing.gutter,
                    end = Spacing.gutter,
                    top = Spacing.sm,
                    bottom = 240.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    SessionIsland(
                        routineName = state.routineName,
                        elapsedSeconds = state.elapsedSeconds,
                        exercisesDone = state.exercisesDone,
                        exerciseCount = state.exercises.size,
                        volumeKg = state.volumeKg,
                        setCount = state.completedSets,
                        prCount = state.prCount
                    )
                }

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

        // Rest timer and finish action float above the list instead of being docked to the bottom
        // edge. The finish button used to live in the header island, at the top of a screen whose
        // whole point is what happens at the bottom of it.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            state.timer?.let { timer ->
                RestTimerIsland(
                    remainingSeconds = timer.remainingSeconds,
                    totalSeconds = timer.totalSeconds,
                    nextSet = nextSetHint(state),
                    onMinus15 = { viewModel.adjustTimer(-15) },
                    onPlus15 = { viewModel.adjustTimer(15) },
                    onSkip = { viewModel.skipTimer() }
                )
            }
            IslandButton(
                text = stringResource(if (editing) R.string.edit_session_save else R.string.active_finish),
                icon = Icons.Outlined.Check,
                onClick = { confirmFinish = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (sessionActionsOpen) {
        SessionActionsSheet(
            playlistUri = state.playlistUri,
            playlistType = state.playlistType,
            onDismiss = { sessionActionsOpen = false }
        )
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
            // Nothing to reorder with a single block (one exercise, or one superset).
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
        // Blocks are dragged, not cards: a superset is a sequence and moves as a whole.
        ReorderSheet(
            title = stringResource(R.string.reorder_title),
            rows = supersetBlocks(state.exercises, supersetLetters),
            onConfirm = { keys -> viewModel.applyOrder(keys.flatMap { it.split(',').map(String::toLong) }) },
            onDismiss = { showReorder = false }
        )
    }

    if (replaceSheetExercise != null) {
        // The row is kept and only the movement changes, so position, superset, rest and notes
        // survive the replacement.
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
        // Title carries the number shown in the table; warmup sets have none, so they fall back
        // to the generic title.
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
        // Finishing is the only way to close a session and cannot be undone, so it is confirmed;
        // the same sheet also adjusts date and duration before they reach the history.
        FinishWorkoutSheet(
            startTime = state.startTime,
            elapsedSeconds = state.elapsedSeconds,
            // A session without completed sets is deleted rather than saved, even if it has
            // exercises.
            isEmpty = state.completedSets == 0 && !editing,
            volumeKg = state.volumeKg,
            setCount = state.completedSets,
            // Discarding the workout sits next to closing it: this is the moment the choice is
            // made, and the confirmation dialog still stands between the two.
            onDelete = if (editing) null else ({ confirmCancel = true }),
            editing = editing,
            onConfirm = { startTime, duration ->
                if (editing) {
                    viewModel.saveEdits(startTime, duration) { kept ->
                        if (kept) onFinished() else onCancelled()
                    }
                    return@FinishWorkoutSheet
                }
                // A deleted session has no summary to show, so exit as if it were cancelled.
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
        // Asked before the session is closed, so it also applies to a workout without completed
        // sets: it never reaches the history, but it may still have changed the routine.
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
        // Cancelling discards the session, completed sets included.
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

/** Reference to a single set inside the session; keys the long-press actions sheet. */
private data class SetRef(val workoutExerciseId: Long, val setId: Long)

/**
 * The session, as the head of the list: a coloured band with the routine, how far along it is and
 * the running clock, over the three numbers being recorded.
 *
 * It scrolls with the exercises instead of sitting fixed above them. A workout is read from the
 * top once and then worked through set by set: keeping the clock pinned cost a fifth of the screen
 * for a number nobody watches while lifting.
 */
@Composable
private fun SessionIsland(
    routineName: String?,
    elapsedSeconds: Int,
    exercisesDone: Int,
    exerciseCount: Int,
    volumeKg: Double,
    setCount: Int,
    prCount: Int
) {
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IslandShape,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        RampBand(
            contentPadding = PaddingValues(horizontal = Spacing.lg + Spacing.xs, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = routineName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.workout_free_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.active_progress, exercisesDone, exerciseCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1
                )
            }
            Text(
                text = formatDuration(elapsedSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            MetricTile(
                icon = Icons.Outlined.FitnessCenter,
                label = stringResource(R.string.stat_volume),
                value = formatVolumeValue(volumeKg),
                unit = stringResource(R.string.unit_kg),
                tint = MetricColors.Volume,
                centered = true,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Outlined.Repeat,
                label = stringResource(R.string.stat_sets),
                value = setCount.toString(),
                tint = MetricColors.Sets,
                centered = true,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Outlined.EmojiEvents,
                label = stringResource(R.string.stat_records),
                value = prCount.toString(),
                tint = MetricColors.Records,
                centered = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * What the header used to carry beside the clock — the playlist and the stopwatch — now behind its
 * one round action. They are used once per workout, and as buttons they took the same room as the
 * clock they sat next to. Discarding the session is not here: it belongs to the sheet that closes
 * the workout, where the alternative to it is on the same line.
 */
@Composable
private fun SessionActionsSheet(
    playlistUri: String?,
    playlistType: PlaylistType?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stopwatch: StopwatchController = koinInject()
    var showStopwatch by remember { mutableStateOf(false) }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.active_actions)) {
        if (playlistType != null && !playlistUri.isNullOrBlank()) {
            SheetActionRow(
                icon = Icons.Outlined.MusicNote,
                label = stringResource(R.string.active_play_playlist_cd),
                onClick = {
                    onDismiss()
                    // Neither a music app nor a browser: say so instead of doing nothing.
                    if (!launchPlaylist(context, playlistUri, playlistType)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.active_playlist_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
        // Same stopwatch as the Dashboard: a count started earlier keeps running here.
        SheetActionRow(
            icon = Icons.Outlined.Timer,
            label = stringResource(R.string.stopwatch_title),
            onClick = { showStopwatch = true }
        )
    }

    if (showStopwatch) {
        StopwatchSheet(controller = stopwatch, onDismiss = { showStopwatch = false })
    }
}

/**
 * Rest timer: a ring counting down, what comes after it, and the controls.
 *
 * The countdown used to be a number over a straight bar. Round, it says the same thing in a third
 * of the width, which is what leaves room on the line for the set waiting at the end of it — the
 * question actually being asked during a rest.
 */
@Composable
private fun RestTimerIsland(
    remainingSeconds: Int,
    totalSeconds: Int,
    nextSet: String?,
    onMinus15: () -> Unit,
    onPlus15: () -> Unit,
    onSkip: () -> Unit
) {
    val island = EinaTheme.island
    IslandSurface(modifier = Modifier.fillMaxWidth(), shape = IslandShape, elevation = 18.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            ActivityRing(
                progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f,
                diameter = 56.dp,
                strokeWidth = 7.dp
            ) {
                Text(
                    text = formatDuration(remainingSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(stringResource(R.string.timer_rest), style = MaterialTheme.typography.titleSmall)
                if (nextSet != null) {
                    Text(
                        text = nextSet,
                        style = MaterialTheme.typography.labelMedium,
                        color = island.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IslandIconButton(
                icon = Icons.Outlined.Remove,
                contentDescription = stringResource(R.string.timer_minus_15),
                onClick = onMinus15,
                containerColor = island.sunken,
                size = 40.dp
            )
            IslandIconButton(
                icon = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.timer_plus_15),
                onClick = onPlus15,
                containerColor = island.sunken,
                size = 40.dp
            )
            IslandIconButton(
                icon = Icons.Outlined.SkipNext,
                contentDescription = stringResource(R.string.timer_skip),
                onClick = onSkip,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                size = 40.dp
            )
        }
    }
}

/**
 * The set the rest is being taken for: the first one still open, with the weight already suggested
 * for it. Null when nothing is left to do, in which case the timer says only "rest".
 */
@Composable
private fun nextSetHint(state: ActiveWorkoutUiState): String? {
    val exercise = state.exercises.firstOrNull { ex -> ex.sets.any { it.completedAt == null } }
        ?: return null
    val set = exercise.sets.first { it.completedAt == null }
    val number = exercise.sets
        .takeWhile { it.id != set.id }
        .count { it.setType.countsAsWorking } + 1
    val weight = (set.weight ?: set.suggestedWeight ?: set.targetWeight)
        ?.takeIf { exercise.weightType.usesWeight }
    return if (weight == null) {
        stringResource(R.string.timer_next_set, number)
    } else {
        stringResource(
            R.string.timer_next_set_value,
            number,
            "${formatDecimal(weight)} ${stringResource(R.string.unit_kg)}"
        )
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
        // The tinted border marks the cards belonging to the same superset.
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (supersetTint == null) Modifier
                else Modifier.border(2.dp, supersetTint, IslandShape)
            ),
        shape = IslandShape,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        onLongClick = onOpenActions
    ) {
        if (supersetLetter != null) {
            SupersetBadge(letter = supersetLetter)
        }

        // The name opens the exercise sheet, so form can be checked without leaving the session.
        Text(
            exercise.name.localized(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                // Small radius: TileShape (24dp) is taller than half a text line and its curve
                // clipped the first and last letters of long names.
                .clip(RoundedCornerShape(8.dp))
                // Clickable children must forward the long press too, otherwise the gesture only
                // works on the few dead spots of the card.
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
            // Only working sets are numbered: a warmup in between shows W and does not consume
            // the number of the set after it.
            var workingNumber = 0
            exercise.sets.forEach { set ->
                if (set.setType.countsAsWorking) workingNumber++
                // Keyed by set id: without it the swipe state belongs to the position, so after a
                // deletion it stays with the row that moved up and the last row can no longer be
                // dragged away.
                key(set.id) {
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
    val hapticTap = LocalHapticTap.current
    val completed = set.completedAt != null

    // The typed text lives in the UI, not in the model: routing every keystroke through Double
    // would turn "52." into "52.0" and make the decimal digit impossible to type.
    var weightText by remember(set.id) { mutableStateOf(set.weight?.toString() ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.actualReps?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(if (completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            // Long press opens the set actions; the numeric fields keep their own taps.
            .combinedClickable(onLongClick = { hapticTap(); onLongClick() }, onClick = {})
            .padding(vertical = Spacing.sm, horizontal = SetRowInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SetColumnGap)
    ) {
        // The set marker is also the control: tapping it opens the W / number / F / D picker.
        SetTypeIndicator(
            type = set.setType,
            number = number,
            isPR = set.isPR,
            onClick = onTypeClick,
            modifier = Modifier.width(SetMarkerWidth)
        )

        PreviousValueText(
            text = set.previous?.let { formatPrevious(it, weightType) } ?: "—",
            modifier = Modifier.weight(previousColumnWeight(weightType))
        )

        // Placeholders are the values suggested by the ViewModel, the same ones recorded when a
        // set is completed without typing anything. Kilograms and kilometres share this field:
        // only the column header changes.
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

/** Set check: an empty circle until done, a filled accent pill once completed. */
@Composable
private fun SetCheckButton(completed: Boolean, onClick: () -> Unit) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    Box(
        modifier = Modifier
            .size(SetCheckSize)
            .clip(PillShape)
            .background(if (completed) MaterialTheme.colorScheme.primary else island.outlineSubtle)
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

/** Actions for an exercise in the current session, opened by long press. */
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

/** Per-exercise note for this session; it does not overwrite the routine note. */
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

internal fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

/** Volume as an integer with thousands separator; the unit is printed by the tile. */
internal fun formatVolumeValue(kg: Double): String =
    "%,d".format(kg.toLong()).replace(',', '.')

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/**
 * "60kg×8" where a load exists, reps (or seconds) alone for bodyweight and timed exercises:
 * printing "0kg×8" on a pull-up would be a made-up value.
 */
private fun formatPrevious(set: com.eina.app.data.db.SetEntryEntity, weightType: WeightType): String {
    val reps = set.actualReps?.toString()
    // For distance the previous value is kilometres and minutes; the compact "5.2km·30" form fits
    // the column, the expanded one would cut off the time.
    if (weightType.usesDistance) {
        val km = set.weight?.let { "${formatDecimal(it)}km" }
        return listOfNotNull(km, reps).joinToString("·").ifBlank { "—" }
    }
    if (!weightType.usesWeight) return reps ?: "—"
    val weight = set.weight?.let { "${formatNumber(it)}kg" }
    return listOfNotNull(weight, reps).joinToString("×").ifBlank { "—" }
}

/**
 * Session exercises as draggable blocks: a superset is a single row, since its members move
 * together. The row key is the joined member ids, so the confirmed order can be flattened back
 * without keeping a separate map.
 */
@Composable
private fun supersetBlocks(
    exercises: List<SessionExerciseUi>,
    letters: Map<Int, String>
): List<ReorderRow> {
    // Context and locale are read outside: composables cannot be called inside the map lambdas.
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
