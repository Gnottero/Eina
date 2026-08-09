package com.eina.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SessionSummaryCard
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.StopwatchController
import com.eina.app.ui.components.StopwatchIconButton
import com.eina.app.ui.components.StopwatchSheet
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.components.weekDayInitials
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.time.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun DashboardScreen(
    onStartWorkoutClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSessionClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
    stopwatch: StopwatchController = koinInject()
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    val stopwatchState by stopwatch.state.collectAsState()
    var showStopwatch by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val hasWeekVolume = state.weekVolumeByDay.any { it > 0f }

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.dashboard_title),
                subtitle = formatFullDate(today),
                trailing = {
                    // Il cronometro sta in testa alla Dashboard: e' un attrezzo da aprire in
                    // fretta, non una voce di menu.
                    StopwatchIconButton(
                        running = stopwatchState.running,
                        onClick = { showStopwatch = true }
                    )
                    IslandIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.dashboard_settings),
                        onClick = onSettingsClick
                    )
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = stringResource(R.string.dashboard_week),
                value = state.weekSessions.toString(),
                unit = pluralStringResource(R.plurals.session_count, state.weekSessions, state.weekSessions)
                    .substringAfter(' '),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(state.weekVolumeKg),
                unit = stringResource(R.string.unit_kg),
                icon = Icons.Outlined.FitnessCenter,
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dashboard_weekly_volume), style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasWeekVolume) {
                    stringResource(R.string.dashboard_weekly_volume_value, formatVolume(state.weekVolumeKg))
                } else {
                    stringResource(R.string.dashboard_no_data)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = state.weekVolumeByDay,
                labels = weekDayInitials(),
                highlightIndex = today.dayOfWeek.value - 1,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(
            title = stringResource(R.string.dashboard_recent),
            actionLabel = stringResource(R.string.dashboard_history),
            actionIcon = Icons.Outlined.History.takeIf { state.recentSessions.isNotEmpty() },
            onAction = onHistoryClick.takeIf { state.recentSessions.isNotEmpty() }
        )

        if (state.recentSessions.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.dashboard_empty_title),
                description = stringResource(R.string.dashboard_empty_description),
                icon = Icons.Outlined.History
            )
        } else {
            state.recentSessions.take(3).forEach { session ->
                SessionSummaryCard(
                    summary = session,
                    onClick = { onSessionClick(session.sessionId) }
                )
            }
        }

        IslandButton(
            text = stringResource(R.string.dashboard_start_workout),
            icon = Icons.Outlined.PlayArrow,
            onClick = onStartWorkoutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showStopwatch) {
        StopwatchSheet(controller = stopwatch, onDismiss = { showStopwatch = false })
    }
}
