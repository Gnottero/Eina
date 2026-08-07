package com.eina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/** Card di riepilogo sessione: data, durata, esercizi, metriche in riga. */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    IslandCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRelativeDay(summary.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = buildString {
                        append(formatTime(summary.startTime))
                        summary.durationMinutes?.let { append(" · ${formatDuration(it)}") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = island.textSecondary
                )
            }
            if (summary.prCount > 0) {
                EinaBadge(
                    text = "${summary.prCount} PR",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = summary.exerciseNames.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            InlineMetric(label = "Serie", value = summary.setCount.toString())
            InlineMetric(label = "Ripetizioni", value = summary.totalReps.toString())
            InlineMetric(label = "Volume", value = "${formatVolume(summary.volumeKg)} kg")
        }
    }
}

@Composable
private fun InlineMetric(label: String, value: String) {
    val island = EinaTheme.island
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = island.textSecondary
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}
