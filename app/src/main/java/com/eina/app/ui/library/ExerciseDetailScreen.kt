package com.eina.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.BodyDiagram
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.categoryFor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
            Text("Caricamento...", color = island.textSecondary)
        }
        return
    }

    IslandScreen(
        header = {
            ScreenHeader(
                title = current.name,
                subtitle = current.equipment?.takeIf { it.isNotBlank() },
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            BodyDiagram(
                muscleGroupsPrimary = current.muscleGroupsPrimary,
                muscleGroupsSecondary = current.muscleGroupsSecondary,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                current.muscleGroupsPrimary.forEach { muscle ->
                    EinaBadge(text = muscle, color = categoryFor(muscle).color, filled = true)
                }
            }
            if (current.muscleGroupsSecondary.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    current.muscleGroupsSecondary.forEach { muscle ->
                        EinaBadge(text = muscle, color = categoryFor(muscle).color)
                    }
                }
            }
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Descrizione", style = MaterialTheme.typography.titleMedium)
            Text(
                current.description,
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Come registrare", style = MaterialTheme.typography.titleMedium)
            Text(
                current.loggingInstructions,
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
        }
    }
}
