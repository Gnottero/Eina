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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.eina.app.R
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
import com.eina.app.ui.components.weekDayInitials
import com.eina.app.ui.library.localized
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel


@Composable
fun ProgressScreen(
    onBodyWeightClick: () -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val hasWeekVolume = state.weekVolumeByDay.any { it > 0f }

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.progress_title),
                subtitle = if (state.totalSessions > 0) {
                    stringResource(
                        R.string.progress_subtitle,
                        pluralStringResource(R.plurals.session_count, state.totalSessions, state.totalSessions),
                        formatVolume(state.totalVolumeKg)
                    )
                } else {
                    stringResource(R.string.progress_current_week)
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandSegmentedRow(
            items = weekDayInitials(),
            secondaryLabels = state.weekDates.map { it.dayOfMonth.toString() },
            selectedIndex = state.selectedDayIndex,
            onSelect = viewModel::selectDay
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(state.selectedDayVolumeKg),
                unit = stringResource(R.string.unit_kg),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stat_sets),
                value = state.selectedDaySets.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.progress_volume_per_day), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (hasWeekVolume) R.string.progress_current_week else R.string.dashboard_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = state.weekVolumeByDay,
                labels = weekDayInitials(),
                highlightIndex = state.selectedDayIndex,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.progress_consistency), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.progress_last_weeks, 18),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            HeatmapCalendar(
                valuesByDay = state.volumeByDay,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = stringResource(R.string.progress_prs))

        if (state.personalRecords.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.progress_pr_empty_title),
                description = stringResource(R.string.progress_pr_empty_description),
                icon = Icons.Outlined.EmojiEvents
            )
        } else {
            state.personalRecords.take(8).forEach { record ->
                PrRow(record = record)
            }
        }

        SectionHeader(title = stringResource(R.string.progress_body))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = stringResource(R.string.stat_weight),
                value = state.latestBodyweightKg?.let { formatDecimal(it) } ?: "–",
                unit = stringResource(R.string.unit_kg),
                icon = Icons.Outlined.MonitorWeight,
                onClick = onBodyWeightClick,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = stringResource(R.string.stat_streak),
                value = state.streakWeeks.toString(),
                unit = pluralStringResource(R.plurals.week_count, state.streakWeeks, state.streakWeeks).substringAfter(' '),
                icon = Icons.Outlined.Whatshot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PrRow(record: PrRecord) {
    val island = EinaTheme.island
    val context = LocalContext.current
    IslandCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.exerciseName.localized(), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = context.formatRelativeDay(record.achievedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
            Text(
                text = context.formatPrValue(record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
