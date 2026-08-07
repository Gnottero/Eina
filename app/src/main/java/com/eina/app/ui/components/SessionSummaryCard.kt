package com.eina.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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

/**
 * Card di riepilogo sessione, in chiave minimale: giorno e ora in testa, una sola riga di
 * metriche separate da punti e i nomi degli esercizi. Niente riquadri dentro la card — la
 * gerarchia la fanno il corpo del testo e lo spazio, non altri contenitori.
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
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = formatRelativeDay(summary.startTime),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (summary.prCount > 0) {
                EinaBadge(
                    text = if (summary.prCount == 1) "1 PR" else "${summary.prCount} PR",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = formatTime(summary.startTime),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
        }

        Text(
            text = listOfNotNull(
                summary.durationMinutes?.let { formatDuration(it) },
                "${formatVolume(summary.volumeKg)} kg",
                if (summary.setCount == 1) "1 serie" else "${summary.setCount} serie"
            ).joinToString(" · "),
            style = MaterialTheme.typography.titleSmall
        )

        if (summary.exerciseNames.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        color = island.textSecondary
                    )
                }
            }
        }
    }
}

/** Quanti esercizi si elencano per esteso prima di riassumere i restanti in "+ altri N". */
private const val MAX_EXERCISE_LINES = 3
