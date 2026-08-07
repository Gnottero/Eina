package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/**
 * Elenco routine. Non-lazy di proposito: le routine sono poche e la schermata che lo contiene
 * e' gia' scrollabile (una LazyColumn annidata in uno scroll verticale non e' misurabile).
 */
@Composable
fun RoutineListScreen(
    onStartSession: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineListViewModel = koinViewModel()
) {
    val routines by viewModel.routines.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (routines.isEmpty()) {
            IslandEmptyState(
                title = "Nessuna routine",
                description = "Crea una routine per avere serie, ripetizioni e recupero gia' pronti.",
                icon = Icons.AutoMirrored.Outlined.ListAlt
            )
        } else {
            routines.forEach { routine ->
                RoutineRow(
                    routine = routine,
                    onStart = { onStartSession(routine.id) },
                    onEdit = { onEditRoutine(routine.id) }
                )
            }
        }
    }
}

@Composable
private fun RoutineRow(routine: RoutineEntity, onStart: () -> Unit, onEdit: () -> Unit) {
    val island = EinaTheme.island
    IslandCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            routine.name.ifBlank { "Routine senza nome" },
            style = MaterialTheme.typography.titleMedium
        )
        routine.notes?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            IslandButton(
                text = "Avvia",
                icon = Icons.Outlined.PlayArrow,
                onClick = onStart,
                modifier = Modifier.weight(1f)
            )
            IslandSecondaryButton(
                text = "Modifica",
                icon = Icons.Outlined.EditNote,
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
