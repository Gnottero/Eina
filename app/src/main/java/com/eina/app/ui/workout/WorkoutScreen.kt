package com.eina.app.ui.workout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel

@Composable
fun WorkoutScreen(
    onSessionStarted: (Long) -> Unit,
    viewModel: WorkoutViewModel = koinViewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { viewModel.startNewSession(onSessionStarted) }) {
            Text("Nuovo allenamento libero", style = MaterialTheme.typography.titleMedium)
        }
    }
}
