package com.eina.app.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.PrRecord
import com.eina.app.ui.components.HeatmapCalendar
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandRow
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSegmentedRow
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.MiniLineChart
import com.eina.app.ui.components.RampBand
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatPrValue
import com.eina.app.ui.components.formatRelativeDay
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.components.weekDayInitials
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.library.localized
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle
import java.time.Month
import java.time.format.TextStyle
import org.koin.androidx.compose.koinViewModel


@Composable
fun ProgressScreen(
    onBodyWeightClick: () -> Unit = {},
    onExerciseClick: (Long) -> Unit = {},
    viewModel: ProgressViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    val locale = currentLocale()

    IslandScreen(
        header = {
            ScreenHeader(
                eyebrow = pluralStringResource(
                    R.plurals.session_count,
                    state.totalSessions,
                    state.totalSessions
                ),
                title = stringResource(R.string.progress_title)
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // One island for the whole answer to "how is it going": the total, the span it is measured
        // over, and the three pictures that explain it. They were four islands with four headers,
        // and the number that matters was not among them.
        IslandCard(
            modifier = Modifier.fillMaxWidth(),
            shape = IslandShape,
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            RampBand(contentPadding = PaddingValues(Spacing.lg + Spacing.xs)) {
                Text(
                    text = stringResource(
                        R.string.progress_total_volume,
                        stringResource(periodLabel(state.period))
                    ).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatVolume(state.periodVolumeKg),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.unit_kg),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(start = Spacing.xs, bottom = 5.dp)
                    )
                }
                Text(
                    text = state.deltaPercent?.let { delta ->
                        if (delta >= 0) {
                            stringResource(R.string.progress_delta_up, delta)
                        } else {
                            stringResource(R.string.progress_delta_down, delta)
                        }
                    } ?: stringResource(R.string.progress_delta_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                IslandSegmentedRow(
                    items = ProgressPeriod.entries.map { stringResource(periodTab(it)) },
                    selectedIndex = state.period.ordinal,
                    onSelect = viewModel::selectPeriod
                )

                ChartBlock(title = stringResource(chartTitle(state.period))) {
                    MiniBarChart(
                        values = state.chartValues,
                        labels = chartLabels(state.period, state.chartValues.size, locale),
                        highlightIndex = state.chartHighlight,
                        height = 84.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ChartBlock(
                    title = stringResource(R.string.progress_days_trained),
                    // The streak keeps its number here instead of a tile of its own: it is a
                    // reading of the very calendar underneath it.
                    trailing = pluralStringResource(
                        R.plurals.week_count,
                        state.streakWeeks,
                        state.streakWeeks
                    )
                ) {
                    HeatmapCalendar(
                        valuesByDay = state.volumeByDay,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                ChartBlock(
                    title = stringResource(R.string.progress_bodyweight),
                    trailing = state.latestBodyweightKg
                        ?.let { "${formatDecimal(it)} ${stringResource(R.string.unit_kg)}" },
                    onClick = onBodyWeightClick
                ) {
                    if (state.bodyweightSeries.size < 2) {
                        Text(
                            text = stringResource(R.string.progress_bodyweight_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = island.textSecondary
                        )
                    } else {
                        MiniLineChart(
                            values = state.bodyweightSeries,
                            lineColor = MetricColors.Bodyweight,
                            height = 84.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.progress_prs), tint = MetricColors.Records)

        if (state.personalRecords.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.progress_pr_empty_title),
                description = stringResource(R.string.progress_pr_empty_description),
                icon = Icons.Outlined.EmojiEvents
            )
        } else {
            IslandCard(
                modifier = Modifier.fillMaxWidth(),
                shape = IslandShape,
                contentPadding = PaddingValues(vertical = Spacing.sm, horizontal = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.personalRecords.take(8).forEach { record ->
                    // A record leads to the exercise detail and its progression chart, the question
                    // that follows "how much did I lift".
                    PrRow(record = record, onClick = { onExerciseClick(record.exerciseId) })
                }
            }
        }
    }
}

/** Titled block inside the hero island: a tiny label, an optional value on the right, a chart. */
@Composable
private fun ChartBlock(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(squircle(14.dp))
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable { hapticTap(); onClick() }
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = island.textSecondary
            )
            if (trailing != null) {
                Text(text = trailing, style = MaterialTheme.typography.titleSmall)
            }
        }
        content()
    }
}

@Composable
private fun PrRow(record: PrRecord, onClick: () -> Unit) {
    val context = LocalContext.current
    IslandRow(
        title = record.exerciseName.localized(),
        subtitle = context.formatRelativeDay(record.achievedAt),
        onClick = onClick,
        trailing = {
            Text(
                text = context.formatPrValue(record),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}

private fun periodLabel(period: ProgressPeriod): Int = when (period) {
    ProgressPeriod.WEEK -> R.string.progress_this_week
    ProgressPeriod.MONTH -> R.string.progress_this_month
    ProgressPeriod.YEAR -> R.string.progress_this_year
}

private fun periodTab(period: ProgressPeriod): Int = when (period) {
    ProgressPeriod.WEEK -> R.string.progress_period_week
    ProgressPeriod.MONTH -> R.string.progress_period_month
    ProgressPeriod.YEAR -> R.string.progress_period_year
}

private fun chartTitle(period: ProgressPeriod): Int = when (period) {
    ProgressPeriod.WEEK -> R.string.progress_per_day
    ProgressPeriod.MONTH -> R.string.progress_per_week
    ProgressPeriod.YEAR -> R.string.progress_per_month
}

/** Bar labels of the period: weekday initials, week numbers, month initials of the active locale. */
@Composable
private fun chartLabels(period: ProgressPeriod, bars: Int, locale: java.util.Locale): List<String> =
    when (period) {
        ProgressPeriod.WEEK -> weekDayInitials()
        // A month is five or six weeks depending on where the first lands, so the labels are
        // counted off the bars rather than assumed.
        ProgressPeriod.MONTH -> (1..bars).map { it.toString() }
        ProgressPeriod.YEAR -> Month.entries.map {
            it.getDisplayName(TextStyle.NARROW, locale)
        }
    }
