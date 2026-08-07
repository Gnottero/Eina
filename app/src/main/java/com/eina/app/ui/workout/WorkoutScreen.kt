package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandIconButton
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.routine.RoutineListScreen
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutScreen(
    onSessionStarted: (Long) -> Unit,
    onCreateRoutineClick: () -> Unit = {},
    onEditRoutineClick: (Long) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val island = EinaTheme.island

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Allena",
                subtitle = "Parti libero o segui una routine",
                trailing = {
                    IslandIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "Nuova routine",
                        onClick = onCreateRoutineClick
                    )
                }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Allenamento libero", style = MaterialTheme.typography.titleMedium)
            Text(
                "Aggiungi esercizi mentre ti alleni, senza pianificare nulla.",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            IslandButton(
                text = "Inizia ora",
                icon = Icons.Outlined.Bolt,
                onClick = { viewModel.startNewSession(onSessionStarted) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(
            title = "Le tue routine",
            actionLabel = "Nuova",
            onAction = onCreateRoutineClick
        )

        RoutineListScreen(
            onStartSession = onSessionStarted,
            onEditRoutine = onEditRoutineClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
