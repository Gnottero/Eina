package com.eina.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.SessionSummaryCard
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
private val weekDayLabels = listOf("L", "M", "M", "G", "V", "S", "D")

@Composable
fun DashboardScreen(
    onStartWorkoutClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSessionClick: (Long) -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    val today = LocalDate.now()
    val hasWeekVolume = state.weekVolumeByDay.any { it > 0f }

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Oggi",
                subtitle = today.format(dateFormatter).replaceFirstChar { it.uppercase() }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Settimana",
                value = state.weekSessions.toString(),
                unit = if (state.weekSessions == 1) "sessione" else "sessioni",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Volume",
                value = formatVolume(state.weekVolumeKg),
                unit = "kg",
                icon = Icons.Outlined.FitnessCenter,
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Volume settimanale", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasWeekVolume) {
                    "${formatVolume(state.weekVolumeKg)} kg sollevati questa settimana"
                } else {
                    "Nessun dato ancora"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = state.weekVolumeByDay,
                labels = weekDayLabels,
                highlightIndex = today.dayOfWeek.value - 1,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(
            title = "Ultimi allenamenti",
            actionLabel = if (state.recentSessions.isNotEmpty()) "Storico" else null,
            onAction = onHistoryClick.takeIf { state.recentSessions.isNotEmpty() }
        )

        if (state.recentSessions.isEmpty()) {
            IslandEmptyState(
                title = "Nessun allenamento registrato",
                description = "Avvia una sessione: qui comparira' il riepilogo dell'ultima, con serie, volume e PR.",
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
            text = "Inizia allenamento",
            icon = Icons.Outlined.PlayArrow,
            onClick = onStartWorkoutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
