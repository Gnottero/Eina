package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.library.currentLocale
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.MetricColors
import com.eina.app.ui.theme.Spacing

/**
 * Minimal session summary card: a header row (name, day, time, chevron), three metric columns with
 * a small label and a large number, and the exercises on one line. No inner boxes and no heavy
 * dividers: hierarchy comes from type size and spacing.
 */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val locale = currentLocale()
    val context = LocalContext.current
    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // The title is the routine name; a free workout says so explicitly rather than showing
            // an empty slot.
            Text(
                text = summary.routineName ?: stringResource(R.string.workout_free_name),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${context.formatRelativeDay(summary.startTime)} · ${formatTime(summary.startTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 1
            )
            if (summary.prCount > 0) {
                EinaBadge(
                    text = pluralStringResource(R.plurals.pr_count, summary.prCount, summary.prCount),
                    color = MetricColors.Records
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = island.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryMetric(
                label = stringResource(R.string.stat_duration),
                value = summary.durationMinutes?.let { formatDuration(it) } ?: "—",
                tint = MetricColors.Duration,
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            SummaryMetric(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(summary.volumeKg),
                unit = stringResource(R.string.unit_kg),
                tint = MetricColors.Volume,
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            SummaryMetric(
                label = stringResource(R.string.stat_sets),
                value = summary.setCount.toString(),
                tint = MetricColors.Sets,
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.exerciseNames.isNotEmpty()) {
            // One line only: the card is a preview, the full list lives in the detail screen.
            Text(
                text = summary.exerciseNames.joinToString(" · ") { it.localized(locale) },
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Metric column: small label on top, large number below with the unit trailing it. */
@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    // The label takes the metric colour and the number stays black: colouring the number too made
    // all three metrics look equally urgent.
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelMedium,
                    // Same colour as the number: the unit is part of it and looked dim in grey.
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/** Vertical hairline between two metrics: separation without boxes. */
@Composable
private fun MetricDivider() {
    val island = EinaTheme.island
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .width(1.dp)
            .height(32.dp)
            .background(island.outlineSubtle)
    )
}
