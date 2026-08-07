package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eina.app.ui.routine.RoutineListScreen
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutScreen(
    onSessionStarted: (Long) -> Unit,
    onCreateRoutineClick: () -> Unit = {},
    onEditRoutineClick: (Long) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutineClick) {
                Icon(Icons.Filled.Add, contentDescription = "Nuova routine")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Button(onClick = { viewModel.startNewSession(onSessionStarted) }) {
                Text("Nuovo allenamento libero", style = MaterialTheme.typography.titleMedium)
            }

            Text("Le tue routine", style = MaterialTheme.typography.titleMedium)

            RoutineListScreen(
                onStartSession = onSessionStarted,
                onEditRoutine = onEditRoutineClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
