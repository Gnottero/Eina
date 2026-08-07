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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.SessionSummary
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing

/**
 * Card di riepilogo sessione in chiave minimale: una riga di intestazione (giorno, ora, chevron),
 * tre metriche incolonnate con etichetta piccola e numero grande, e gli esercizi su una riga sola.
 * Nessun riquadro interno e nessun divisore marcato: la gerarchia la fanno corpo del testo e spazio.
 */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    IslandCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Il titolo e' il nome della routine: dice cosa si e' fatto. Senza routine
            // (allenamento libero) lo si scrive esplicitamente, non si lascia il vuoto.
            Text(
                text = summary.routineName ?: stringResource(R.string.workout_free_name),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Quando: sul lato destro, in secondo piano rispetto al nome.
            Text(
                text = "${context.formatRelativeDay(summary.startTime)} · ${formatTime(summary.startTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 1
            )
            if (summary.prCount > 0) {
                EinaBadge(
                    text = pluralStringResource(R.plurals.pr_count, summary.prCount, summary.prCount),
                    color = MaterialTheme.colorScheme.primary
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
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            SummaryMetric(
                label = stringResource(R.string.stat_volume),
                value = formatVolume(summary.volumeKg),
                unit = stringResource(R.string.unit_kg),
                modifier = Modifier.weight(1f)
            )
            MetricDivider()
            SummaryMetric(
                label = stringResource(R.string.stat_sets),
                value = summary.setCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.exerciseNames.isNotEmpty()) {
            // Una riga sola: la card e' un'anteprima, l'elenco completo sta nel dettaglio.
            Text(
                text = summary.exerciseNames.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Colonna metrica: etichetta piccola sopra, numero grande sotto con l'unita' in coda. */
@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val island = EinaTheme.island
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
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
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

/** Filo verticale fra due metriche: separa senza disegnare riquadri. */
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
