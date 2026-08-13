package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.eina.app.R
import com.eina.app.data.transfer.RoutineTransfer
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandCardHeader
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.routine.RoutineListScreen
import com.eina.app.ui.routine.RoutineListViewModel
import com.eina.app.ui.routine.readRoutineFile
import com.eina.app.ui.theme.EinaTheme
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
    val island = EinaTheme.island
    val context = LocalContext.current
    val activeSession by viewModel.activeSession.collectAsState()
    val routineListViewModel: RoutineListViewModel = koinViewModel()
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

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.workout_title),
                subtitle = stringResource(R.string.workout_subtitle),
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
                        onClick = onCreateRoutineClick
                    )
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        val active = activeSession

        if (active != null) {
            // The workout clock runs outside its own screen too: measured from startTime, so the
            // banner shows the real duration and not a paused counter.
            var now by remember(active.id) { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(active.id) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1000)
                }
            }
            val elapsedSeconds = ((now - active.startTime) / 1000).coerceAtLeast(0)

            // With a session open the only action is resuming it: two parallel workouts would make
            // it ambiguous where the recorded sets belong.
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workout_in_progress_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formatElapsed(elapsedSeconds),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    stringResource(R.string.workout_in_progress_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = island.textSecondary
                )
                IslandButton(
                    text = stringResource(R.string.workout_resume),
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { onSessionStarted(active.id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                IslandCardHeader(
                    title = stringResource(R.string.workout_free_title),
                    icon = Icons.Outlined.Bolt,
                    subtitle = stringResource(R.string.workout_free_description)
                )
                IslandButton(
                    text = stringResource(R.string.workout_start_now),
                    icon = Icons.Outlined.Bolt,
                    onClick = { viewModel.startNewSession(onSessionStarted) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // No action here: routine creation lives only in the header "+".
        SectionHeader(title = stringResource(R.string.workout_your_routines))

        RoutineListScreen(
            onStartSession = onSessionStarted,
            onEditRoutine = onEditRoutineClick,
            startBlocked = active != null,
            modifier = Modifier.fillMaxWidth()
        )
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
