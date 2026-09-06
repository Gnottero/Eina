package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.primaryCategoryFor
import com.eina.app.ui.theme.squircle

/**
 * Session summary card: one identifying line — coloured dot, routine name, when — over a sunken
 * block holding the three numbers a past workout is remembered by.
 *
 * The numbers used to sit loose on the white card, which made the card as tall as the hero of the
 * Dashboard for three figures. Boxed together they read as one measurement strip, and the card is
 * short enough that three of them fit under the hero without scrolling.
 */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    // The dot carries the muscle group the session worked most: the only thing that tells two
    // routine names apart before they are read.
    val dotColor = primaryCategoryFor(listOfNotNull(summary.dominantMuscle)).color

    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            // The title is the routine name; a free workout says so explicitly rather than showing
            // an empty slot.
            Text(
                text = summary.routineName ?: stringResource(R.string.workout_free_name),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (summary.prCount > 0) {
                EinaBadge(
                    text = pluralStringResource(R.plurals.pr_count, summary.prCount, summary.prCount),
                    color = MetricColors.Records
                )
            }
            Text(
                text = "${context.formatRelativeDay(summary.startTime)} · ${formatTime(summary.startTime)}",
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(squircle(16.dp))
                .background(island.sunken)
                .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            SummaryMetric(
                label = stringResource(R.string.stat_duration),
                value = summary.durationMinutes?.let { formatDuration(it) } ?: "—",
                tint = MetricColors.Duration,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(summary.volumeKg),
                unit = stringResource(R.string.unit_kg),
                tint = MetricColors.Volume,
                modifier = Modifier.weight(1f)
            )
            SummaryMetric(
                label = stringResource(R.string.stat_sets),
                value = summary.setCount.toString(),
                tint = MetricColors.Sets,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Metric column: tiny label over a large number. Here the number takes the colour and the label
 * stays grey — the opposite of a metric tile, because inside the strip there is no icon to carry
 * the colour and three black numbers in a row read as a table.
 */
@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val island = EinaTheme.island
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = island.textSecondary,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
