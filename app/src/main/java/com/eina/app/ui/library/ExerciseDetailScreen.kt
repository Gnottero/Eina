package com.eina.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.usesDistance
import com.eina.app.data.db.usesDuration
import com.eina.app.data.db.usesWeight
import com.eina.app.domain.ProgressPoint
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.ExerciseAnimation
import com.eina.app.ui.components.hasExerciseMedia
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.MiniLineChart
import com.eina.app.ui.components.formatDayMonth
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.categoryFor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: (() -> Unit)? = null,
    viewModel: ExerciseDetailViewModel = koinViewModel(parameters = { parametersOf(exerciseId) })
) {
    val exercise by viewModel.exercise.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val island = EinaTheme.island
    val current = exercise

    if (current == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.loading), color = island.textSecondary)
        }
        return
    }

    IslandScreen(
        header = {
            ScreenHeader(
                title = current.localizedName(),
                subtitle = current.equipment?.takeIf { it.isNotBlank() }?.let { equipmentLabel(it) },
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (hasExerciseMedia(current.mediaUri)) {
            IslandCard(modifier = Modifier.fillMaxWidth()) {
                ExerciseAnimation(mediaUri = current.mediaUri, modifier = Modifier.fillMaxWidth())
            }
        }

        // The animation already shows the worked muscles coloured on the figure; only the labels
        // remain here, for whoever wants the group name.
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            // FlowRow and not Row: exercises like the clean have five secondary muscles, and on a
            // single line the last one was clipped off screen.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                current.muscleGroupsPrimary.forEach { muscle ->
                    EinaBadge(text = muscleLabel(muscle), color = categoryFor(muscle).color, filled = true)
                }
            }
            if (current.muscleGroupsSecondary.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    current.muscleGroupsSecondary.forEach { muscle ->
                        EinaBadge(text = muscleLabel(muscle), color = categoryFor(muscle).color)
                    }
                }
            }
        }

        ProgressionCard(weightType = current.weightType, points = progress)

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exercise_description), style = MaterialTheme.typography.titleMedium)
            Text(
                current.localizedDescription(),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.exercise_how_to_log), style = MaterialTheme.typography.titleMedium)
            Text(
                current.localizedLoggingInstructions(),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }
    }
}

/**
 * Progression of the exercise over time, as for bodyweight: one line per quantity, one point per
 * workout (the best set, see [com.eina.app.domain.exerciseProgress]).
 *
 * The two quantities are the ones its set table records: load and reps, seconds for timed
 * exercises, kilometres and minutes for distance. Where one is not recorded the chart is omitted
 * rather than drawing a flat line at zero.
 */
@Composable
private fun ProgressionCard(weightType: WeightType, points: List<ProgressPoint>) {
    val island = EinaTheme.island

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(stringResource(R.string.exercise_progress_title), style = MaterialTheme.typography.titleMedium)

        if (points.isEmpty()) {
            Text(
                text = stringResource(R.string.exercise_progress_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            return@IslandCard
        }

        val labels = listOf(formatDayMonth(points.first().date), formatDayMonth(points.last().date))

        if (weightType.usesWeight || weightType.usesDistance) {
            ProgressionChart(
                label = stringResource(
                    if (weightType.usesDistance) R.string.exercise_progress_distance
                    else R.string.exercise_progress_weight
                ),
                values = points.map { (it.weight ?: 0.0).toFloat() },
                labels = labels
            )
        }

        ProgressionChart(
            label = stringResource(
                when {
                    weightType.usesDuration -> R.string.exercise_progress_seconds
                    weightType.usesDistance -> R.string.exercise_progress_minutes
                    else -> R.string.exercise_progress_reps
                }
            ),
            values = points.map { (it.reps ?: 0).toFloat() },
            labels = labels
        )
    }
}

@Composable
private fun ProgressionChart(label: String, values: List<Float>, labels: List<String>) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = EinaTheme.island.textSecondary
    )
    MiniLineChart(values = values, labels = labels, modifier = Modifier.fillMaxWidth())
}
