package com.eina.app.ui.history

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.eina.app.R
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking
import com.eina.app.data.db.usesDecimalField
import com.eina.app.domain.Superset
import com.eina.app.domain.totalVolume
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.LocalButtonShape
import com.eina.app.ui.components.SheetButtonShape
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSurface
import com.eina.app.ui.components.MetricTile
import com.eina.app.ui.components.IslandTextField
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.SetTableHeader
import com.eina.app.ui.components.SetTypeIndicator
import com.eina.app.ui.components.MiniLineChart
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.SupersetBadge
import com.eina.app.ui.components.supersetColor
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.localized
import com.eina.app.ui.share.ShareCardData
import com.eina.app.ui.share.copyImageToClipboard
import com.eina.app.ui.share.isInstagramInstalled
import com.eina.app.ui.share.needsLegacyStoragePermission
import com.eina.app.ui.share.openInstagramStoryCamera
import com.eina.app.ui.share.renderShareCard
import com.eina.app.ui.share.saveImageToGallery
import com.eina.app.ui.share.saveShareImage
import com.eina.app.ui.share.shareCardDataOf
import com.eina.app.ui.share.shareImage
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onRoutineCreated: (Long) -> Unit = {},
    justFinished: Boolean = false,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) }
) {
    val state by viewModel.uiState.collectAsState()
    val summary = state.summary
    val context = LocalContext.current
    var shareData by remember { mutableStateOf<ShareCardData?>(null) }
    var editingNotes by remember { mutableStateOf(false) }

    if (editingNotes) {
        SessionNotesSheet(
            notes = state.notes,
            onSave = { viewModel.setNotes(it) },
            onDismiss = { editingNotes = false }
        )
    }

    shareData?.let { data ->
        ShareSheet(
            data = data,
            fileName = "eina-allenamento-$sessionId.png",
            onDismiss = { shareData = null }
        )
    }

    IslandScreen(
        header = {
            // Same header island as the workout screen: a summary is that screen with the
            // recording taken away, so the shell it lives in must not be a different one.
            SummaryHeader(
                title = if (justFinished) {
                    stringResource(R.string.session_completed)
                } else {
                    summary?.let { formatFullDate(it.startTime) } ?: stringResource(R.string.session_fallback_title)
                },
                subtitle = summary?.let { session ->
                    buildString {
                        // Short date right after a workout: the long form wrapped the subtitle and
                        // pushed the duration onto a second line.
                        if (justFinished) append("${formatDayMonth(session.startTime)} · ")
                        append(formatTime(session.startTime))
                        session.durationMinutes?.let { append(" · ${formatDuration(it)}") }
                        session.routineName?.let { append(" · $it") }
                    }
                },
                volumeKg = summary?.volumeKg ?: 0.0,
                setCount = summary?.setCount ?: 0,
                onBack = onBack,
                onEdit = if (summary == null) null else onEdit,
                onShare = if (summary == null) null else {
                    { shareData = shareCardDataOf(summary) }
                },
                modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md)
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (summary == null) {
            IslandEmptyState(
                title = stringResource(R.string.session_empty_title),
                description = stringResource(R.string.session_empty_description),
                icon = Icons.Outlined.History
            )
            return@IslandScreen
        }

        // Right after a workout the streak comes first.
        if (justFinished) {
            StreakCard(weeks = state.streakWeeks)
        }

        // Watch data, when present: shown in the summary only, never on the shareable card.
        state.vitals?.let { vitals -> VitalsCard(vitals = vitals) }

        // A comment on the workout: how it went, how one felt. Always shown, empty as an
        // invitation to write, so it is one tap away right after finishing.
        SessionNotesCard(notes = state.notes, onClick = { editingNotes = true })

        // Same reading as the workout screen: letters follow the order supersets appear in.
        val supersetLetters = Superset.letters(state.exercises.map { it.supersetGroup })
        state.exercises.forEach { exercise ->
            ExerciseSummaryCard(
                exercise = exercise,
                supersetLetter = exercise.supersetGroup?.let { supersetLetters[it] }
            )
        }

        // A performed workout is already a plan: it is saved as a routine and the editor opens on
        // it, which is where the name is changed.
        if (state.exercises.isNotEmpty()) {
            val routineName = summary.routineName ?: formatFullDate(summary.startTime)
            val createdMessage = stringResource(R.string.history_create_routine_done)
            // Filled pill with the accent ramp, the same button that ends a workout: it is the
            // one action of the page and read as a disabled tile in its sunken form.
            IslandButton(
                text = stringResource(R.string.history_create_routine),
                icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                onClick = {
                    viewModel.createRoutine(routineName) { routineId ->
                        if (routineId != null) {
                            Toast.makeText(context, createdMessage, Toast.LENGTH_SHORT).show()
                            onRoutineCreated(routineId)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Header island of the summary: the one the workout screen uses, with the live parts removed —
 * no running clock, no progress bar, no finish button. Back, correct and share are the round
 * buttons of the top row; volume and sets are the two headline tiles.
 */
@Composable
private fun SummaryHeader(
    title: String,
    subtitle: String?,
    volumeKg: Double,
    setCount: Int,
    onBack: () -> Unit,
    onEdit: (() -> Unit)?,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val island = EinaTheme.island
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
                IslandIconButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    containerColor = island.sunken,
                    size = 40.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = island.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onEdit != null) {
                    IslandIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.session_edit_cd),
                        onClick = onEdit,
                        containerColor = island.sunken,
                        size = 40.dp
                    )
                }
                if (onShare != null) {
                    IslandIconButton(
                        icon = Icons.Outlined.Share,
                        contentDescription = stringResource(R.string.session_share_cd),
                        onClick = onShare,
                        containerColor = island.sunken,
                        size = 40.dp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                MetricTile(
                    icon = Icons.Outlined.FitnessCenter,
                    label = stringResource(R.string.stat_volume),
                    value = formatVolume(volumeKg),
                    unit = stringResource(R.string.unit_kg),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.Repeat,
                    label = stringResource(R.string.stat_sets),
                    value = setCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Free comment on the workout, written and re-read from the summary. With nothing written the card
 * still stands, showing the prompt: an empty state here is the button to fill it.
 */
@Composable
private fun SessionNotesCard(notes: String?, onClick: () -> Unit) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        onClick = { hapticTap(); onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Notes,
                contentDescription = null,
                tint = island.textSecondary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.session_notes_title),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = notes?.takeIf { it.isNotBlank() } ?: stringResource(R.string.session_notes_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = if (notes.isNullOrBlank()) island.textSecondary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Editor for the workout comment. */
@Composable
private fun SessionNotesSheet(
    notes: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(notes.orEmpty()) }

    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.session_notes_title)) {
        IslandTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.session_notes_field),
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

/** Heart rate and calories read from Health Connect for the workout window. */
@Composable
private fun VitalsCard(vitals: SessionVitals) {
    val island = EinaTheme.island
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = MetricColors.Heart,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.session_vitals_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (vitals.avgBpm != null) {
                StatTile(
                    label = stringResource(R.string.stat_heart_rate_avg),
                    value = vitals.avgBpm.toString(),
                    unit = stringResource(R.string.unit_bpm),
                    // Max sits under the tile label: a second heart tile would crowd out calories.
                    description = vitals.maxBpm?.let { stringResource(R.string.stat_heart_rate_max, it) },
                    tint = MetricColors.Heart,
                    modifier = Modifier.weight(1f)
                )
            }
            if (vitals.kcal != null) {
                StatTile(
                    label = stringResource(R.string.stat_calories),
                    value = formatVolume(vitals.kcal),
                    unit = stringResource(R.string.unit_kcal),
                    tint = MetricColors.Calories,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (vitals.samples.size > 1) {
            MiniLineChart(
                values = vitals.samples.map { it.bpm.toFloat() },
                lineColor = MetricColors.Heart,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Streak of consecutive weeks with at least one workout, highlighted after finishing. */
@Composable
private fun StreakCard(weeks: Int) {
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = pluralStringResource(R.plurals.session_streak_weeks, weeks, weeks),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = if (weeks <= 1) {
                        stringResource(R.string.session_streak_start)
                    } else {
                        stringResource(R.string.session_streak_ongoing)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * One work block of the session: the exercise card of the workout screen with the recording taken
 * away — same shape, same name in the accent, same table — but no fields, no check button and no
 * "add set", since nothing here can be typed into. Blocks are keyed by workoutExerciseId, so the
 * same exercise repeated in a session appears twice.
 */
@Composable
private fun ExerciseSummaryCard(
    exercise: SessionExerciseDetail,
    supersetLetter: String?
) {
    val island = EinaTheme.island
    // workingSets and totalVolume walk the list on every call; the sets of a recorded workout no
    // longer change, so both are computed once.
    val working = remember(exercise) { exercise.workingSets }
    val volume = remember(exercise) { totalVolume(exercise.sets) }
    val supersetTint = supersetLetter?.let { supersetColor(it) }

    IslandCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (supersetTint == null) Modifier
                else Modifier.border(2.dp, supersetTint, IslandShape)
            ),
        shape = IslandShape,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        if (supersetLetter != null) {
            SupersetBadge(letter = supersetLetter)
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = exercise.exerciseName.localized(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.set_count, working.size, working.size))
                    if (volume > 0.0) append(" · ${formatVolume(volume)} kg")
                },
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SetTableHeader(
                weightType = exercise.weightType,
                showPrevious = false,
                trailingSlot = false
            )
            // As in the session: only working sets are numbered, warmups show W.
            var workingNumber = 0
            exercise.sets.forEach { set ->
                if (set.setType.countsAsWorking) workingNumber++
                SetRow(number = workingNumber, set = set)
            }
        }
    }
}

/** Set row of the summary: same columns as the recording table, read-only. */
@Composable
private fun SetRow(number: Int, set: CompletedSetRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        // Number or type letter, the same marker used in the session table; a record shows PR.
        SetTypeIndicator(
            type = set.setType,
            number = number,
            isPR = set.isPR,
            modifier = Modifier.width(40.dp)
        )
        if (set.weightType.usesDecimalField) {
            SetValueText(text = decimalLabel(set), modifier = Modifier.weight(1f))
        }
        SetValueText(text = "${set.actualReps ?: 0}", modifier = Modifier.weight(1f))
    }
}

/** Recorded value: the field of the recording table with the input taken away. */
@Composable
private fun SetValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier.padding(vertical = Spacing.xs)
    )
}

/**
 * Decimal column of a recorded set: kilograms, or kilometres for distance. Loads added to or taken
 * off bodyweight keep their sign, which is the whole meaning of the number.
 */
private fun decimalLabel(set: CompletedSetRow): String {
    val value = formatDecimal(set.weight ?: 0.0)
    return when (set.weightType) {
        WeightType.BODYWEIGHT_PLUS_LOAD -> "+$value"
        WeightType.ASSISTED -> "-$value"
        else -> value
    }
}

/**
 * Share sheet: a single image, with a transparent-background toggle.
 *
 * With a background the image stands on its own anywhere. Without one only the text remains: it is
 * saved to the gallery (and copied to the clipboard), then Instagram opens so the user can pick
 * their own photo and add it as a sticker. This detour is the only way to combine a personal photo
 * with the stats, since Instagram's ADD_TO_STORY intent accepts a sticker but imposes the
 * background.
 */
@Composable
private fun ShareSheet(
    data: ShareCardData,
    fileName: String,
    onDismiss: () -> Unit
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    val hapticTap = LocalHapticTap.current
    var transparent by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(transparent) {
        bitmap = withContext(Dispatchers.Default) { renderShareCard(context, data, transparent) }
    }

    // Up to Android 9 writing to the gallery needs an explicit permission; once granted, the save
    // resumes on its own.
    var pendingSave by remember { mutableStateOf(false) }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val current = bitmap
        if (granted && pendingSave && current != null) {
            context.saveOverlay(current, fileName, openInstagram = true)
            onDismiss()
        } else if (!granted) {
            context.toast(R.string.share_permission_needed)
        }
        pendingSave = false
    }

    fun saveAndOpen() {
        val current = bitmap ?: return
        if (needsLegacyStoragePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingSave = true
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        context.saveOverlay(current, fileName, openInstagram = true)
        onDismiss()
    }

    // Floating menu as well: buttons take the sheet shape, not the pill one.
    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalButtonShape provides SheetButtonShape) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium)

            // Dark backdrop behind the preview: with no background the image is white text on
            // nothing and would be invisible on the light surface.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(IslandShape)
                    .background(if (transparent) PreviewBackdrop else island.sunken),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = stringResource(R.string.session_preview_cd),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    stringResource(R.string.share_transparent_toggle),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = transparent,
                    onCheckedChange = { hapticTap(); transparent = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = island.textSecondary.copy(alpha = 0.25f),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            if (transparent) {
                Text(
                    stringResource(R.string.share_transparent_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }

            if (isInstagramInstalled(context)) {
                IslandButton(
                    text = stringResource(R.string.share_save_and_open),
                    onClick = { saveAndOpen() },
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IslandSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                IslandSecondaryButton(
                    text = stringResource(R.string.action_more),
                    onClick = {
                        val current = bitmap ?: return@IslandSecondaryButton
                        val uri = saveShareImage(context, current, fileName)
                        shareImage(context, uri, text = context.getString(R.string.session_share_text))
                        onDismiss()
                    },
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        }
    }
}

/** Neutral dark grey, standing in for the photo the user will put under the overlay. */
private val PreviewBackdrop = Color(0xFF2B2B2B)

/**
 * Saves the overlay to the gallery and copies it to the clipboard, so both routes inside Instagram
 * work: picking it from the gallery as a sticker, or pasting it.
 */
private fun Context.saveOverlay(bitmap: Bitmap, fileName: String, openInstagram: Boolean) {
    val galleryUri = saveImageToGallery(this, bitmap, fileName)
    if (galleryUri == null) {
        toast(R.string.share_save_failed)
        return
    }
    // The clipboard copy goes through the FileProvider: a MediaStore Uri is not readable by
    // another app without an explicit grant.
    val copied = copyImageToClipboard(this, saveShareImage(this, bitmap, fileName), getString(R.string.app_name))
    toast(if (copied) R.string.share_copied else R.string.share_saved)

    if (openInstagram && !openInstagramStoryCamera(this)) {
        toast(R.string.share_instagram_missing)
    }
}

private fun Context.toast(@StringRes message: Int) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

