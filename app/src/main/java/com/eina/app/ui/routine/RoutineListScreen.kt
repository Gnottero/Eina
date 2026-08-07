package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.EinaCard
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoutineListScreen(
    onStartSession: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineListViewModel = koinViewModel()
) {
    val routines by viewModel.routines.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(routines, key = { it.id }) { routine ->
            RoutineRow(
                routine = routine,
                onStart = { onStartSession(routine.id) },
                onEdit = { onEditRoutine(routine.id) }
            )
        }
    }
}

@Composable
private fun RoutineRow(routine: RoutineEntity, onStart: () -> Unit, onEdit: () -> Unit) {
    EinaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(routine.name.ifBlank { "Routine senza nome" }, style = MaterialTheme.typography.titleMedium)
            routine.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onStart) { Text("Avvia") }
                OutlinedButton(onClick = onEdit) { Text("Modifica") }
            }
        }
    }
}
