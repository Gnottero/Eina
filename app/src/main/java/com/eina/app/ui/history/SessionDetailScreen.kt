package com.eina.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.components.formatDecimal
import com.eina.app.ui.components.formatDuration
import com.eina.app.ui.components.formatFullDate
import com.eina.app.ui.components.formatTime
import com.eina.app.ui.components.formatVolume
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) }
) {
    val island = EinaTheme.island
    val state by viewModel.uiState.collectAsState()
    val summary = state.summary

    IslandScreen(
        header = {
            ScreenHeader(
                title = summary?.let { formatFullDate(it.startTime) } ?: "Allenamento",
                subtitle = summary?.let { session ->
                    buildString {
                        append(formatTime(session.startTime))
                        session.durationMinutes?.let { append(" · ${formatDuration(it)}") }
                    }
                },
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (summary == null) {
            IslandEmptyState(
                title = "Sessione senza serie completate",
                description = "Questo allenamento non ha serie registrate.",
                icon = Icons.Outlined.History
            )
            return@IslandScreen
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Volume",
                value = formatVolume(summary.volumeKg),
                unit = "kg",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Serie",
                value = summary.setCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        state.exercises.forEach { exercise ->
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                Text(exercise.exerciseName, style = MaterialTheme.typography.titleMedium)
                exercise.sets.forEach { set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = "${set.setIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = island.textSecondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = setLabel(set),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (set.isWarmup) {
                            EinaBadge(text = "Riscaldamento", color = island.textSecondary)
                        }
                        if (set.isPR) {
                            EinaBadge(text = "PR", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun setLabel(set: CompletedSetRow): String = when (set.weightType) {
    WeightType.TIME_BASED -> "${set.actualReps ?: 0} s"
    WeightType.BODYWEIGHT -> "${set.actualReps ?: 0} rip."
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        "${set.actualReps ?: 0} rip. · +${formatDecimal(set.weight ?: 0.0)} kg"
    WeightType.ASSISTED ->
        "${set.actualReps ?: 0} rip. · -${formatDecimal(set.weight ?: 0.0)} kg"
    else -> "${set.actualReps ?: 0} rip. · ${formatDecimal(set.weight ?: 0.0)} kg"
}
