package com.eina.app.ui.workout

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.data.transfer.RoutineTransfer
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSurface
import com.eina.app.ui.components.MetricTile
import com.eina.app.ui.components.RampBand
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.routine.RoutineListSection
import com.eina.app.ui.routine.RoutineListViewModel
import com.eina.app.ui.routine.readRoutineFile
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.PillShape
import com.eina.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutScreen(
    onSessionStarted: (Long) -> Unit,
    onCreateRoutineClick: () -> Unit = {},
    onEditRoutineClick: (Long) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val openSession by viewModel.openSession.collectAsState()
    val routineListViewModel: RoutineListViewModel = koinViewModel()
    val routines by routineListViewModel.routines.collectAsState()
    val importDone = stringResource(R.string.routine_import_done)
    val importFailed = stringResource(R.string.routine_import_failed)

    // System picker: the routine arrives as a file, so no storage permission is needed.
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val json = uri?.let { readRoutineFile(context, it) }
        if (json == null) {
            if (uri != null) Toast.makeText(context, importFailed, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        routineListViewModel.importRoutine(json) { imported ->
            Toast.makeText(context, if (imported) importDone else importFailed, Toast.LENGTH_SHORT).show()
        }
    }

    val active = activeSession
    val routineCount = pluralStringResource(R.plurals.routine_count, routines.size, routines.size)

    IslandScreen(
        header = {
            ScreenHeader(
                // The count of what is on screen, plus the one thing that changes what the screen
                // offers: an open session.
                eyebrow = if (active != null) {
                    stringResource(R.string.workout_eyebrow_open, routineCount)
                } else {
                    routineCount
                },
                title = stringResource(R.string.workout_title),
                trailing = {
                    IslandIconButton(
                        icon = Icons.Outlined.FileDownload,
                        contentDescription = stringResource(R.string.routine_import),
                        onClick = { importPicker.launch(arrayOf(RoutineTransfer.MIME_TYPE, "*/*")) },
                        containerColor = EinaTheme.island.sunken,
                        contentColor = EinaTheme.island.textSecondary
                    )
                    IslandIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.workout_new_routine),
                        onClick = onCreateRoutineClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        // With a session open the free workout is not offered: two parallel workouts would make it
        // ambiguous where the recorded sets belong.
        floatingBottom = if (active == null) {
            {
                IslandButton(
                    text = stringResource(R.string.workout_free_title),
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { viewModel.startNewSession(onSessionStarted) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            null
        }
    ) {
        if (active != null) {
            OpenSessionBanner(
                startTime = active.startTime,
                sessionId = active.id,
                summary = openSession,
                onResume = { onSessionStarted(active.id) }
            )
        }

        RoutineListSection(
            routines = routines,
            onStartSession = { routineId ->
                routineListViewModel.startSession(routineId, onSessionStarted)
            },
            onEditRoutine = onEditRoutineClick,
            onDeleteRoutine = routineListViewModel::deleteRoutine,
            onExportRoutine = routineListViewModel::exportRoutine,
            startBlocked = active != null
        )
    }
}

/**
 * The open session, as the head of its own island: a coloured band with the running clock and the
 * round button that goes back to it, over the three numbers recorded so far.
 *
 * It used to be a white card with a clock and a button — one island among many, saying nothing
 * about what was already in the session. The band makes it the one thing on the page that is warm.
 */
@Composable
private fun OpenSessionBanner(
    startTime: Long,
    sessionId: Long,
    summary: OpenSessionUi?,
    onResume: () -> Unit
) {
    // The workout clock runs outside its own screen too: measured from startTime, so the banner
    // shows the real duration and not a paused counter.
    var now by remember(sessionId) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sessionId) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsedSeconds = ((now - startTime) / 1000).coerceAtLeast(0)

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IslandShape,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        RampBand(
            contentPadding = PaddingValues(Spacing.lg + Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.workout_session_open).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = formatElapsed(elapsedSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.workout_in_progress_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                // White on the ramp rather than another ramp: the only round button on a coloured
                // surface has to be the light one, or it disappears into it.
                IslandSurface(
                    modifier = Modifier.size(60.dp),
                    shape = PillShape,
                    elevation = 6.dp,
                    onClick = onResume
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = stringResource(R.string.workout_resume),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(26.dp)
                    )
                }
            }
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
                value = formatVolumeValue(summary?.volumeKg ?: 0.0),
                unit = stringResource(R.string.unit_kg),
                tint = MetricColors.Volume,
                centered = true,
                // On the ramp island the three numbers sit straight on the white: a sunken tile
                // each read as three grey boxes hung under a coloured band.
                showBackground = false,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Outlined.Repeat,
                label = stringResource(R.string.stat_sets),
                value = (summary?.setCount ?: 0).toString(),
                tint = MetricColors.Sets,
                centered = true,
                showBackground = false,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                icon = Icons.Outlined.EmojiEvents,
                label = stringResource(R.string.stat_records),
                value = (summary?.prCount ?: 0).toString(),
                tint = MetricColors.Records,
                centered = true,
                showBackground = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Duration of the running workout: hh:mm:ss past an hour, mm:ss below. */
private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
