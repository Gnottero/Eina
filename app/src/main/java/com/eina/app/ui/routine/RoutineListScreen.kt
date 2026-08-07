package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
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
    startBlocked: Boolean = false,
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
                    // Avviare una routine crea una NUOVA sessione: passare direttamente routine.id
                    // apriva la sessione con quell'id, cioe' un allenamento vecchio gia' svolto.
                    onStart = { viewModel.startSession(routine.id, onStartSession) },
                    onEdit = { onEditRoutine(routine.id) },
                    onDelete = { viewModel.deleteRoutine(routine) },
                    startEnabled = !startBlocked
                )
            }
        }
    }
}

@Composable
private fun RoutineRow(
    routine: RoutineEntity,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    startEnabled: Boolean
) {
    val island = EinaTheme.island
    val name = routine.name.ifBlank { "Routine senza nome" }
    var actionsOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Una routine ha una sola azione ovvia: avviarla. Modifica ed eliminazione stanno nel foglio
    // che si apre col tocco lungo, cosi' la card resta un bottone grande e basta.
    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        onLongClick = { actionsOpen = true }
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        routine.notes?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = island.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            IslandButton(
                text = "Avvia",
                icon = Icons.Outlined.PlayArrow,
                onClick = onStart,
                enabled = startEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (actionsOpen) {
        IslandBottomSheet(onDismiss = { actionsOpen = false }, title = name) {
            SheetActionRow(
                icon = Icons.Outlined.EditNote,
                label = "Modifica routine",
                onClick = { actionsOpen = false; onEdit() }
            )
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = "Elimina routine",
                description = "Gli allenamenti gia' registrati restano nello storico",
                destructive = true,
                onClick = { actionsOpen = false; confirmDelete = true }
            )
        }
    }

    if (confirmDelete) {
        // L'eliminazione non e' annullabile: si conferma prima di toccare il database.
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = IslandShape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Eliminare \"$name\"?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("La routine e i suoi esercizi pianificati verranno rimossi.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Elimina", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Annulla", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
