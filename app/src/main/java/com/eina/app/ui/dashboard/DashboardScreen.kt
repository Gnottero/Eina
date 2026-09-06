package com.eina.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.components.ActivityRing
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.MetricTile
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SessionSummaryCard
import com.eina.app.ui.components.StopwatchController
import com.eina.app.ui.components.StopwatchIconButton
import com.eina.app.ui.components.StopwatchSheet
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.components.weekDayInitials
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import java.time.LocalDate
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun DashboardScreen(
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

    IslandScreen(
        header = {
            ScreenHeader(
                // The date sits above the title as a tiny line, not below as a subtitle: it is
                // context, so the large title stays the anchor.
                eyebrow = formatFullDate(today),
                title = stringResource(R.string.dashboard_title),
                trailing = {
                    // The stopwatch lives in the header: a tool to reach fast, not a menu entry.
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
        // One hero island for the whole week: the ring and the sentence that reads it, the two
        // numbers, and the chart of the days. They used to be three separate islands saying the
        // same thing, and the first screen of the app was a stack of white rectangles.
        IslandCard(
            modifier = Modifier.fillMaxWidth(),
            shape = IslandShape,
            contentPadding = PaddingValues(Spacing.lg + Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityRing(
                    // Against the goal and no longer against seven: a ring that cannot be closed
                    // is a progress bar that always looks late.
                    progress = if (state.weekGoalDays == 0) 0f else {
                        state.weekDaysTrained.toFloat() / state.weekGoalDays
                    },
                    diameter = 112.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.weekDaysTrained.toString(),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = stringResource(R.string.dashboard_ring_goal, state.weekGoalDays)
                                .uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = island.textSecondary
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.day_count,
                            state.weekDaysTrained,
                            state.weekDaysTrained
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            state.weekDaysTrained == 0 -> stringResource(R.string.dashboard_goal_none)
                            state.goalDaysLeft == 0 -> stringResource(R.string.dashboard_goal_done)
                            else -> pluralStringResource(
                                R.plurals.dashboard_goal_left,
                                state.goalDaysLeft,
                                state.goalDaysLeft
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = island.textSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                MetricTile(
                    icon = Icons.Outlined.FitnessCenter,
                    label = stringResource(R.string.dashboard_week_volume),
                    value = formatVolume(state.weekVolumeKg),
                    unit = stringResource(R.string.unit_kg),
                    tint = MetricColors.Volume,
                    centered = true,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    icon = Icons.Outlined.EmojiEvents,
                    label = stringResource(R.string.dashboard_week_prs),
                    value = state.weekPrCount.toString(),
                    tint = MetricColors.Records,
                    centered = true,
                    modifier = Modifier.weight(1f)
                )
            }

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
            state.recentSessions.forEach { session ->
                SessionSummaryCard(
                    summary = session,
                    onClick = { onSessionClick(session.sessionId) }
                )
            }
        }
    }

    if (showStopwatch) {
        StopwatchSheet(controller = stopwatch, onDismiss = { showStopwatch = false })
    }
}
