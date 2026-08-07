package com.eina.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eina.app.data.db.RoutineEntity
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandIconButton
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
            routines.forEach { card ->
                RoutineRow(
                    card = card,
                    // Avviare una routine crea una NUOVA sessione: passare direttamente routine.id
                    // apriva la sessione con quell'id, cioe' un allenamento vecchio gia' svolto.
                    onStart = { viewModel.startSession(card.routine.id, onStartSession) },
                    onEdit = { onEditRoutine(card.routine.id) },
                    onDelete = { viewModel.deleteRoutine(card.routine) },
                    startEnabled = !startBlocked
                )
            }
        }
    }
}

/**
 * Card routine minimale: nome, una riga di contenuto ("5 esercizi · 18 serie"), l'elenco degli
 * esercizi in grigio e un solo bottone tondo per avviare. Modifica ed eliminazione restano nel
 * foglio che si apre col tocco lungo, cosi' la card resta pulita.
 */
@Composable
private fun RoutineRow(
    card: RoutineCardUi,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    startEnabled: Boolean
) {
    val island = EinaTheme.island
    val routine: RoutineEntity = card.routine
    val name = routine.name.ifBlank { "Routine senza nome" }
    var actionsOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    IslandCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        // Tocco sulla card: apre la routine (solo il tondo a destra la avvia).
        onClick = onEdit,
        onLongClick = { actionsOpen = true }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summaryLine(card),
                    style = MaterialTheme.typography.labelMedium,
                    color = island.textSecondary,
                    maxLines = 1
                )
            }
            IslandIconButton(
                icon = Icons.Outlined.PlayArrow,
                contentDescription = "Avvia $name",
                onClick = { if (startEnabled) onStart() },
                size = 48.dp,
                containerColor = if (startEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    island.sunken
                },
                contentColor = if (startEnabled) Color.White else island.textSecondary
            )
        }

        if (card.exerciseNames.isNotEmpty()) {
            Text(
                text = card.exerciseNames.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        routine.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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

/** "5 esercizi · 18 serie", oppure l'invito a riempirla se e' ancora vuota. */
private fun summaryLine(card: RoutineCardUi): String {
    if (card.exerciseCount == 0) return "Nessun esercizio"
    val exercises = if (card.exerciseCount == 1) "1 esercizio" else "${card.exerciseCount} esercizi"
    val sets = if (card.setCount == 1) "1 serie" else "${card.setCount} serie"
    return "$exercises · $sets"
}
