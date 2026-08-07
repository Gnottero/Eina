package com.eina.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandNumberField
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.MiniLineChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatRelativeDay
import com.eina.app.ui.components.sanitizeWeightInput
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/**
 * Peso corporeo: ogni misura salvata qui diventa il bodyweightSnapshotKg delle set completate
 * successivamente (vedi WorkoutRepository.completeSet), quindi alimenta PR e volume a corpo libero.
 */
@Composable
fun BodyWeightScreen(
    onBack: () -> Unit,
    viewModel: BodyWeightViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    // Il grafico legge dal piu' vecchio al piu' recente: la lista arriva ordinata al contrario.
    val chronological = state.metrics.reversed()

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Peso corporeo",
                subtitle = state.latestKg?.let { "Ultima misura ${formatDecimal(it)} kg" }
                    ?: "Nessuna misura registrata",
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Nuova misura", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IslandNumberField(
                    value = input,
                    onValueChange = { new -> input = sanitizeWeightInput(input, new) },
                    label = "kg",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                IslandButton(
                    text = "Salva",
                    onClick = {
                        viewModel.addMeasurement(input)
                        input = ""
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (chronological.isNotEmpty()) {
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                Text("Andamento", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.deltaKg?.let { delta ->
                        val sign = if (delta >= 0) "+" else "-"
                        "$sign${formatDecimal(kotlin.math.abs(delta))} kg dall'ultima misura"
                    } ?: "Prima misura registrata",
                    style = MaterialTheme.typography.bodyMedium,
                    color = island.textSecondary
                )
                MiniLineChart(
                    values = chronological.map { it.bodyweightKg.toFloat() },
                    labels = listOf(
                        formatDayMonth(chronological.first().date),
                        formatDayMonth(chronological.last().date)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionHeader(title = "Storico misure")

        if (state.metrics.isEmpty()) {
            IslandEmptyState(
                title = "Nessuna misura",
                description = "Registra il peso: verra' usato per calcolare volume e PR degli esercizi a corpo libero.",
                icon = Icons.Outlined.MonitorWeight
            )
        } else {
            state.metrics.forEach { metric ->
                IslandCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${formatDecimal(metric.bodyweightKg)} kg",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = formatRelativeDay(metric.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = island.textSecondary
                            )
                        }
                        IslandIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = "Elimina misura",
                            onClick = { viewModel.delete(metric.id) },
                            containerColor = island.sunken
                        )
                    }
                }
            }
        }
    }
}
