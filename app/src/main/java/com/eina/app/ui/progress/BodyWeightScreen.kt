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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.eina.app.R
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
 * Bodyweight: every measurement saved here becomes the bodyweightSnapshotKg of the sets completed
 * afterwards (see WorkoutRepository.completeSet), so it feeds bodyweight PRs and volume.
 */
@Composable
fun BodyWeightScreen(
    onBack: () -> Unit,
    viewModel: BodyWeightViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }

    // The chart reads oldest to newest; the list arrives in the opposite order.
    val chronological = state.metrics.reversed()

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.bodyweight_title),
                subtitle = state.latestKg?.let { stringResource(R.string.bodyweight_subtitle_last, formatDecimal(it)) }
                    ?: stringResource(R.string.bodyweight_subtitle_none),
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bodyweight_new), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IslandNumberField(
                    value = input,
                    onValueChange = { new -> input = sanitizeWeightInput(input, new) },
                    label = stringResource(R.string.unit_kg),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                IslandButton(
                    text = stringResource(R.string.action_save),
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
                Text(stringResource(R.string.bodyweight_trend), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.deltaKg?.let { delta ->
                        val sign = if (delta >= 0) "+" else "-"
                        stringResource(
                            R.string.bodyweight_delta,
                            "$sign${formatDecimal(kotlin.math.abs(delta))}"
                        )
                    } ?: stringResource(R.string.bodyweight_first),
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

        SectionHeader(title = stringResource(R.string.bodyweight_history))

        if (state.metrics.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.bodyweight_empty_title),
                description = stringResource(R.string.bodyweight_empty_description),
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
                                text = context.formatRelativeDay(metric.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = island.textSecondary
                            )
                        }
                        IslandIconButton(
                            icon = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.bodyweight_delete_cd),
                            onClick = { viewModel.delete(metric.id) },
                            containerColor = island.sunken
                        )
                    }
                }
            }
        }
    }
}
