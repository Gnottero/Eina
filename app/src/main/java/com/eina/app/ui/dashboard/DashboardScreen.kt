package com.eina.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.ui.components.ActivityRing
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandCardHeader
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
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
                // La data sta sopra il titolo come riga minuscola, non sotto come sottotitolo:
                // e' contesto, non un secondo titolo, e cosi' il titolo grande resta l'ancora.
                eyebrow = formatFullDate(today),
                title = stringResource(R.string.dashboard_title),
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
        // Hero: un'isola sola con l'anello dei giorni allenati e, accanto, i numeri della
        // settimana. Prima erano due tile affiancate di pari peso e la schermata si apriva senza
        // un punto di ingresso: ora l'occhio cade sull'anello e da li' scende ai numeri.
        IslandCard(
            modifier = Modifier.fillMaxWidth(),
            shape = IslandShape,
            contentPadding = PaddingValues(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    ActivityRing(progress = state.weekDaysTrained / 7f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.weekDaysTrained.toString(),
                                style = MaterialTheme.typography.displayMedium
                            )
                            Text(
                                text = stringResource(R.string.dashboard_ring_total),
                                style = MaterialTheme.typography.labelMedium,
                                color = island.textSecondary
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.dashboard_ring_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = island.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_hero_week).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MetricColors.Volume
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatVolume(state.weekVolumeKg),
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            text = stringResource(R.string.unit_kg),
                            style = MaterialTheme.typography.labelLarge,
                            // Stesso colore del numero: l'unita' ne fa parte.
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = Spacing.xs, bottom = 5.dp)
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.session_count,
                            state.weekSessions,
                            state.weekSessions
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = island.textSecondary
                    )
                }
            }
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            IslandCardHeader(
                title = stringResource(R.string.dashboard_weekly_volume),
                icon = Icons.Outlined.FitnessCenter,
                tint = MetricColors.Volume,
                subtitle = if (hasWeekVolume) {
                    stringResource(R.string.dashboard_weekly_volume_value, formatVolume(state.weekVolumeKg))
                } else {
                    stringResource(R.string.dashboard_no_data)
                }
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
