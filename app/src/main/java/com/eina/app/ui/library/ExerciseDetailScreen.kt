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
import com.eina.app.ui.components.BodyDiagram
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.ExerciseAnimation
import com.eina.app.ui.components.hasExerciseMedia
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandScreen
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

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            BodyDiagram(
                muscleGroupsPrimary = current.muscleGroupsPrimary,
                muscleGroupsSecondary = current.muscleGroupsSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            // FlowRow e non Row: esercizi come "Girata (clean)" hanno cinque muscoli secondari
            // e su una riga sola l'ultimo finiva tagliato fuori dallo schermo.
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
