package com.eina.app.ui.routine

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.exerciseName
import com.eina.app.data.db.usesDecimalField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.SetType
import com.eina.app.data.db.countsAsWorking
import com.eina.app.ui.components.SetTableHeader
import com.eina.app.ui.components.SetTypeIndicator
import com.eina.app.ui.components.SetTypeSheet
import com.eina.app.ui.components.SetValueField
import com.eina.app.ui.components.SwipeToDeleteSetRow
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.components.ExercisePickerSheet
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.ReorderRow
import com.eina.app.ui.components.ReorderSheet
import com.eina.app.ui.components.RestTimeSheet
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.components.SupersetBadge
import com.eina.app.ui.components.SupersetOption
import com.eina.app.ui.components.SupersetSheet
import com.eina.app.ui.components.supersetColor
import com.eina.app.ui.components.formatClock
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.domain.Superset
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.library.localized
import com.eina.app.ui.library.localizedName
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onBack: (() -> Unit)? = null,
    onOpenExercise: (Long) -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: RoutineEditorViewModel = koinViewModel(parameters = { parametersOf(routineId) })
) {
    val uiState by viewModel.uiState.collectAsState()
    val routineExercises by viewModel.routineExercises.collectAsState()
    val exercises by viewModel.exercises.collectAsState()
    val availableExercises by viewModel.availableExercises.collectAsState()
    val routineSets by viewModel.routineSets.collectAsState()
    // Same sheet as the workout screen: adding an exercise works the same everywhere.
    var showPicker by remember { mutableStateOf(false) }
    var showReorder by remember { mutableStateOf(false) }

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
                // Playback lives in the running workout, not in the editor.
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
            // Superset letters follow the order the groups appear in the routine.
            val supersetLetters = Superset.letters(routineExercises.map { it.supersetGroup })
            val supersetOptions = routineExercises
                .filter { it.supersetGroup != null }
                .groupBy { it.supersetGroup!! }
                .map { (group, members) ->
                    SupersetOption(
                        group = group,
                        letter = supersetLetters[group].orEmpty(),
                        members = members.map { exercises[it.exerciseId]?.localizedName() ?: "…" }
                    )
                }
                .sortedBy { it.letter }

            routineExercises.forEach { routineExercise ->
                RoutineExerciseCard(
                    routineExercise = routineExercise,
                    sets = routineSets[routineExercise.id].orEmpty(),
                    exerciseName = exercises[routineExercise.exerciseId]?.localizedName() ?: "…",
                    weightType = exercises[routineExercise.exerciseId]?.weightType ?: WeightType.FREE_WEIGHT,
                    supersetLetter = routineExercise.supersetGroup?.let { supersetLetters[it] },
                    supersetOptions = supersetOptions,
                    onOpenExercise = { onOpenExercise(routineExercise.exerciseId) },
                    onSupersetChange = { group -> viewModel.setSupersetGroup(routineExercise, group) },
                    onNewSuperset = { viewModel.setSupersetGroup(routineExercise, viewModel.nextSupersetGroup()) },
                    onRestChange = { seconds -> viewModel.updateRestSeconds(routineExercise, seconds) },
                    onAddSet = { viewModel.addSet(routineExercise) },
                    onRemoveSet = { set -> viewModel.removeSet(set) },
                    onSetValuesChange = { set, reps, weight -> viewModel.updateSetValues(set, reps, weight) },
                    onSetTypeChange = { set, type -> viewModel.setSetType(set, type) },
                    onNotesChange = { notes -> viewModel.updateNotes(routineExercise, notes) },
                    availableExercises = availableExercises,
                    canReorder = routineExercises.size > 1,
                    onReorder = { showReorder = true },
                    onReplace = { picked -> viewModel.replaceExercise(routineExercise, picked.id) },
                    onRemove = { viewModel.removeExercise(routineExercise) }
                )
            }
        }

        IslandSecondaryButton(
            text = stringResource(R.string.action_add_exercise),
            icon = Icons.Outlined.Add,
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth()
        )

        IslandButton(
            text = stringResource(R.string.action_save_routine),
            onClick = { viewModel.save(onSaved) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = availableExercises,
            onPick = {
                viewModel.addExercise(it.id)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    if (showReorder) {
        // Blocks are dragged: a superset is a sequence and moves as a whole.
        ReorderSheet(
            title = stringResource(R.string.reorder_title),
            rows = routineBlocks(routineExercises, exercises),
            onConfirm = { keys -> viewModel.applyOrder(keys.flatMap { it.split(',').map(String::toLong) }) },
            onDismiss = { showReorder = false }
        )
    }
}

/**
 * Routine exercises as draggable blocks, as in the workout screen: the row key is the joined
 * member ids, so the confirmed order can be flattened back without a separate map.
 */
@Composable
private fun routineBlocks(
    routineExercises: List<RoutineExerciseEntity>,
    exercises: Map<Long, ExerciseEntity>
): List<ReorderRow> {
    // Composables cannot be called inside the map lambdas: name and locale are resolved here.
    val context = LocalContext.current
    val locale = currentLocale()
    val letters = Superset.letters(routineExercises.map { it.supersetGroup })
    return Superset.blocksOf(routineExercises.map { Superset.Member(it.id, it.supersetGroup) })
        .map { block ->
            val members = block.mapNotNull { member -> routineExercises.find { it.id == member.id } }
            val names = members.map { exercises[it.exerciseId]?.exerciseName()?.localized(locale) ?: "…" }
            val letter = members.firstOrNull()?.supersetGroup?.let { letters[it] }
            ReorderRow(
                key = members.joinToString(",") { it.id.toString() },
                title = if (letter == null) names.firstOrNull().orEmpty() else context.getString(R.string.superset_badge, letter),
                subtitle = if (letter == null) null else names.joinToString(" · "),
                tint = letter?.let { supersetColor(it) }
            )
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineExerciseCard(
    routineExercise: RoutineExerciseEntity,
    sets: List<RoutineSetEntity>,
    exerciseName: String,
    weightType: WeightType,
    supersetLetter: String?,
    supersetOptions: List<SupersetOption>,
    onOpenExercise: () -> Unit,
    onSupersetChange: (Int?) -> Unit,
    onNewSuperset: () -> Unit,
    onRestChange: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (RoutineSetEntity) -> Unit,
    onSetValuesChange: (RoutineSetEntity, Int?, Double?) -> Unit,
    onSetTypeChange: (RoutineSetEntity, SetType) -> Unit,
    onNotesChange: (String?) -> Unit,
    availableExercises: List<ExerciseEntity>,
    canReorder: Boolean,
    onReorder: () -> Unit,
    onReplace: (ExerciseEntity) -> Unit,
    onRemove: () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    val supersetTint = supersetLetter?.let { supersetColor(it) }
    var actionsOpen by remember { mutableStateOf(false) }
    var restSheetOpen by remember { mutableStateOf(false) }
    var notesSheetOpen by remember { mutableStateOf(false) }
    var supersetSheetOpen by remember { mutableStateOf(false) }
    var replaceSheetOpen by remember { mutableStateOf(false) }
    var setActionsFor by remember { mutableStateOf<Long?>(null) }
    var setTypeFor by remember { mutableStateOf<Long?>(null) }

    // Same card as the workout screen: no inline buttons, actions live in the long-press sheet.
    IslandCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (supersetTint == null) Modifier
                else Modifier.border(2.dp, supersetTint, IslandShape)
            ),
        shape = IslandShape,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        onLongClick = { actionsOpen = true }
    ) {
        if (supersetLetter != null) {
            SupersetBadge(letter = supersetLetter)
        }

        Text(
            exerciseName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onLongClick = { hapticTap(); actionsOpen = true },
                    onClick = { hapticTap(); onOpenExercise() }
                )
                .padding(vertical = Spacing.xs)
        )

        routineExercise.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
        }

        // Rest is not typed but picked with wheels, like an alarm clock.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .clip(PillShape)
                .background(island.sunken)
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

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            // No "previous" column and no check: this is planning, not recording.
            SetTableHeader(weightType = weightType, showPrevious = false, trailingSlot = false)
            var workingNumber = 0
            sets.forEach { set ->
                if (set.setType.countsAsWorking) workingNumber++
                SwipeToDeleteSetRow(onDelete = { onRemoveSet(set) }) {
                    RoutineSetRow(
                        set = set,
                        number = workingNumber,
                        weightType = weightType,
                        onValuesChange = { reps, weight -> onSetValuesChange(set, reps, weight) },
                        onTypeClick = { setTypeFor = set.id },
                        onLongClick = { setActionsFor = set.id }
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
                    onLongClick = { hapticTap(); actionsOpen = true },
                    onClick = { hapticTap(); onAddSet() }
                )
                .padding(vertical = Spacing.md)
        )
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
                icon = Icons.Outlined.Add,
                label = stringResource(R.string.active_add_set),
                onClick = { actionsOpen = false; onAddSet() }
            )
            SheetActionRow(
                icon = Icons.Outlined.Repeat,
                label = stringResource(R.string.superset_action),
                description = supersetLetter?.let { stringResource(R.string.superset_badge, it) }
                    ?: stringResource(R.string.superset_action_none),
                onClick = { actionsOpen = false; supersetSheetOpen = true }
            )
            SheetActionRow(
                icon = Icons.Outlined.SwapHoriz,
                label = stringResource(R.string.action_replace_exercise),
                description = stringResource(R.string.routine_replace_exercise_description),
                onClick = { actionsOpen = false; replaceSheetOpen = true }
            )
            if (canReorder) {
                SheetActionRow(
                    icon = Icons.Outlined.SwapVert,
                    label = stringResource(R.string.action_reorder),
                    description = stringResource(R.string.reorder_action_description),
                    onClick = { actionsOpen = false; onReorder() }
                )
            }
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.action_remove_exercise),
                destructive = true,
                onClick = { actionsOpen = false; onRemove() }
            )
        }
    }

    setTypeFor?.let { setId ->
        val current = sets.find { it.id == setId }
        if (current == null) {
            setTypeFor = null
        } else {
            SetTypeSheet(
                current = current.setType,
                onSelect = { type -> onSetTypeChange(current, type) },
                onDismiss = { setTypeFor = null }
            )
        }
    }

    setActionsFor?.let { setId ->
        val target = sets.find { it.id == setId }
        // Title carries the number shown in the table; warmups have none and use the generic one.
        val number = sets.takeWhile { it.id != setId }.count { it.setType.countsAsWorking } + 1
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
                    target?.let(onRemoveSet)
                    setActionsFor = null
                }
            )
        }
    }

    if (replaceSheetOpen) {
        // Sets, rest, note and superset are kept: only the movement changes.
        ExercisePickerSheet(
            exercises = availableExercises,
            title = stringResource(R.string.action_replace_exercise),
            onPick = { picked ->
                onReplace(picked)
                replaceSheetOpen = false
            },
            onDismiss = { replaceSheetOpen = false }
        )
    }

    if (restSheetOpen) {
        RestTimeSheet(
            currentSeconds = routineExercise.restSeconds,
            onConfirm = onRestChange,
            onDismiss = { restSheetOpen = false },
            description = if (supersetLetter != null) {
                stringResource(R.string.superset_rest_description)
            } else {
                stringResource(R.string.rest_description_routine)
            }
        )
    }

    if (supersetSheetOpen) {
        SupersetSheet(
            exerciseName = exerciseName,
            current = routineExercise.supersetGroup,
            options = supersetOptions,
            onSelect = onSupersetChange,
            onNewGroup = onNewSuperset,
            onDismiss = { supersetSheetOpen = false }
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

/**
 * Row of a planned set: the workout table without the "previous" column and the check. The marker
 * on the left opens the set type picker.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineSetRow(
    set: RoutineSetEntity,
    number: Int,
    weightType: WeightType,
    onValuesChange: (Int?, Double?) -> Unit,
    onTypeClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val hapticTap = LocalHapticTap.current
    // The typed text lives in the UI, not in the model: routing every keystroke through Double
    // would turn "52." into "52.0" and make the decimal digit impossible to type.
    var weightText by remember(set.id) { mutableStateOf(set.targetWeight?.let { formatTargetWeight(it) } ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.targetReps?.toString() ?: "") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .combinedClickable(onLongClick = { hapticTap(); onLongClick() }, onClick = {})
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        SetTypeIndicator(
            type = set.setType,
            number = number,
            isPR = false,
            onClick = onTypeClick,
            modifier = Modifier.width(40.dp)
        )

        // Kilograms for loads, kilometres for distance exercises: the same decimal field.
        if (weightType.usesDecimalField) {
            SetValueField(
                value = weightText,
                placeholder = null,
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
            placeholder = null,
            onValueChange = { typed ->
                repsText = typed.filter { it.isDigit() }.take(4)
                onValuesChange(repsText.toIntOrNull(), weightText.toDoubleOrNull())
            },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Target weight without a pointless decimal, so the field does not start as "60.0". */
private fun formatTargetWeight(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/** Routine note, inherited by every workout started from it. */
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.playlist_none)) }, onClick = { onSelected(null); expanded = false })
            DropdownMenuItem(text = { Text("Spotify") }, onClick = { onSelected(PlaylistType.SPOTIFY); expanded = false })
            DropdownMenuItem(text = { Text("YouTube Music") }, onClick = { onSelected(PlaylistType.YOUTUBE_MUSIC); expanded = false })
        }
    }
}
