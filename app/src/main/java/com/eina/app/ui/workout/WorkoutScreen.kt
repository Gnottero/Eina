package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.routine.RoutineListScreen
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
    val activeSession by viewModel.activeSession.collectAsState()

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.workout_title),
                subtitle = stringResource(R.string.workout_subtitle),
                trailing = {
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
            // Il tempo dell'allenamento scorre anche fuori dalla sua schermata: si misura da
            // startTime, quindi il banner mostra la durata reale, non un contatore in pausa.
            var now by remember(active.id) { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(active.id) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1000)
                }
            }
            val elapsedSeconds = ((now - active.startTime) / 1000).coerceAtLeast(0)

            // Con una sessione aperta l'unica azione possibile e' rientrarci: due allenamenti
            // in parallelo renderebbero ambiguo dove finiscono le serie registrate.
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
                Text(stringResource(R.string.workout_free_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.workout_free_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = island.textSecondary
                )
                IslandButton(
                    text = stringResource(R.string.workout_start_now),
                    icon = Icons.Outlined.Bolt,
                    onClick = { viewModel.startNewSession(onSessionStarted) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Nessuna azione qui: la creazione routine sta solo nel "+" dell'header, un punto solo.
        SectionHeader(title = stringResource(R.string.workout_your_routines))

        RoutineListScreen(
            onStartSession = onSessionStarted,
            onEditRoutine = onEditRoutineClick,
            startBlocked = active != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Durata dell'allenamento in corso: hh:mm:ss oltre l'ora, mm:ss sotto. */
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
