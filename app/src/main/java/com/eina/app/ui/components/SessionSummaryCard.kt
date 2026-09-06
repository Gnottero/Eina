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

/**
 * Session summary card: coloured dot, routine name with the day under it, over the three
 * numbers a past workout is remembered by.
 *
 * The numbers sit loose on the white card: the sunken strip that used to hold them was the only
 * grey block in a list of white cards, and it read as a table inside a card. The name and the day
 * are stacked rather than pushed to the two ends of one line: squeezed side by side a long
 * routine name was ellipsised to make room for a date that is always the same width, and the card
 * was so low that a list of workouts read as a table of figures.
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
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // The title is the routine name; a free workout says so explicitly rather than
                // showing an empty slot.
                Text(
                    text = summary.routineName ?: stringResource(R.string.workout_free_name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${context.formatRelativeDay(summary.startTime)} · ${formatTime(summary.startTime)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    maxLines = 1
                )
            }
            if (summary.prCount > 0) {
                EinaBadge(
                    text = pluralStringResource(R.plurals.pr_count, summary.prCount, summary.prCount),
                    color = MetricColors.Records
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs),
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
 * stays grey — the opposite of a metric tile, because here there is no icon to carry the colour
 * and three black numbers in a row read as a table.
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
