package com.eina.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/** Quanti esercizi si elencano per esteso prima di riassumere i restanti in "+ altri N". */
private const val MAX_EXERCISE_LINES = 3

/**
 * Card di riepilogo sessione. Le metriche stanno in tre riquadri incassati con numero grande:
 * a colpo d'occhio si legge quanto e' durato, quanto si e' sollevato e quante serie sono state
 * fatte, senza dover decifrare una riga di testo continua.
 */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatRelativeDay(summary.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTime(summary.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
            if (summary.prCount > 0) {
                EinaBadge(
                    text = if (summary.prCount == 1) "1 PR" else "${summary.prCount} PR",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            MetricBox(label = "Durata", value = summary.durationMinutes?.let { formatDuration(it) } ?: "—")
            MetricBox(label = "Volume", value = formatVolume(summary.volumeKg), unit = "kg")
            MetricBox(label = "Serie", value = summary.setCount.toString())
        }

        if (summary.exerciseNames.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                summary.exerciseNames.take(MAX_EXERCISE_LINES).forEach { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = island.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val hidden = summary.exerciseNames.size - MAX_EXERCISE_LINES
                if (hidden > 0) {
                    Text(
                        text = "+ altri $hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.MetricBox(label: String, value: String, unit: String? = null) {
    val island = EinaTheme.island
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(TileShape)
            .background(island.sunken)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
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
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.labelSmall,
                    color = island.textSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}
