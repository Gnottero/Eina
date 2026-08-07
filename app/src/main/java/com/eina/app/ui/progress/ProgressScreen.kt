package com.eina.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eina.app.domain.PrRecord
import com.eina.app.ui.components.HeatmapCalendar
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSegmentedRow
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatPrValue
import com.eina.app.ui.components.formatRelativeDay
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

private val dayInitials = listOf("L", "M", "M", "G", "V", "S", "D")

@Composable
fun ProgressScreen(
    onBodyWeightClick: () -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    val hasWeekVolume = state.weekVolumeByDay.any { it > 0f }

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Progressi",
                subtitle = if (state.totalSessions > 0) {
                    "${state.totalSessions} sessioni · ${formatVolume(state.totalVolumeKg)} kg totali"
                } else {
                    "Settimana corrente"
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandSegmentedRow(
            items = dayInitials,
            secondaryLabels = state.weekDates.map { it.dayOfMonth.toString() },
            selectedIndex = state.selectedDayIndex,
            onSelect = viewModel::selectDay
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Volume",
                value = formatVolume(state.selectedDayVolumeKg),
                unit = "kg",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Serie",
                value = state.selectedDaySets.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Volume per giorno", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasWeekVolume) "Settimana corrente" else "Nessun dato ancora",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = state.weekVolumeByDay,
                labels = dayInitials,
                highlightIndex = state.selectedDayIndex,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Costanza", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Ultime 18 settimane",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            HeatmapCalendar(
                valuesByDay = state.volumeByDay,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = "Record personali")

        if (state.personalRecords.isEmpty()) {
            IslandEmptyState(
                title = "Ancora nessun PR",
                description = "Completa le serie durante un allenamento: i nuovi record compaiono qui.",
                icon = Icons.Outlined.EmojiEvents
            )
        } else {
            state.personalRecords.take(8).forEach { record ->
                PrRow(record = record)
            }
        }

        SectionHeader(title = "Corpo")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Peso",
                value = state.latestBodyweightKg?.let { formatDecimal(it) } ?: "–",
                unit = "kg",
                icon = Icons.Outlined.MonitorWeight,
                onClick = onBodyWeightClick,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Streak",
                value = state.streakDays.toString(),
                unit = if (state.streakDays == 1) "giorno" else "giorni",
                icon = Icons.Outlined.Whatshot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PrRow(record: PrRecord) {
    val island = EinaTheme.island
    IslandCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.exerciseName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = formatRelativeDay(record.achievedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
            Text(
                text = formatPrValue(record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
