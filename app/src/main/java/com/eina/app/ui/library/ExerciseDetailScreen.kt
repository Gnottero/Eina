package com.eina.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.BodyDiagram
import com.eina.app.ui.components.EinaBadge
import com.eina.app.ui.components.EinaCard
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.categoryFor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    viewModel: ExerciseDetailViewModel = koinViewModel(parameters = { parametersOf(exerciseId) })
) {
    val exercise by viewModel.exercise.collectAsState()
    val current = exercise

    if (current == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Caricamento...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(current.name, style = MaterialTheme.typography.headlineSmall)

        BodyDiagram(
            muscleGroupsPrimary = current.muscleGroupsPrimary,
            muscleGroupsSecondary = current.muscleGroupsSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            current.muscleGroupsPrimary.forEach { muscle ->
                EinaBadge(text = muscle, color = categoryFor(muscle).color)
            }
        }
        if (current.muscleGroupsSecondary.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                current.muscleGroupsSecondary.forEach { muscle ->
                    EinaBadge(text = muscle, color = categoryFor(muscle).color.copy(alpha = 0.6f))
                }
            }
        }

        EinaCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Descrizione", style = MaterialTheme.typography.titleMedium)
                Text(current.description, style = MaterialTheme.typography.bodyMedium)
            }
        }

        HorizontalDivider()

        EinaCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text("Come registrare", style = MaterialTheme.typography.titleMedium)
                Text(current.loggingInstructions, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
