package com.eina.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.R
import com.eina.app.domain.RoutineChange
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.formatClock
import com.eina.app.ui.library.localized
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.TileShape

/**
 * A fine allenamento: la sessione e' partita da una scheda ma non l'ha seguita, e la domanda che
 * viene naturale e' "la scheda era sbagliata o era solo oggi?". La si fa qui, una volta sola, con
 * l'elenco di cosa e' cambiato — senza, l'unico modo di aggiornare la scheda era rifare a mano in
 * editor le stesse modifiche appena fatte in palestra.
 *
 * Non fare niente e' la scelta piu' probabile, quindi e' quella del tasto secondario, ma nessuna
 * delle due e' distruttiva: la scheda si puo' sempre correggere a mano.
 */
@Composable
fun RoutineSyncSheet(
    routineName: String?,
    changes: List<RoutineChange>,
    onUpdate: () -> Unit,
    onKeep: () -> Unit
) {
    val island = EinaTheme.island
    IslandBottomSheet(
        // Il foglio non si chiude toccando fuori: la domanda va risposta, altrimenti si resta
        // sull'allenamento gia' chiuso senza sapere cosa e' successo alla scheda.
        onDismiss = onKeep,
        title = stringResource(R.string.routine_sync_title),
        scrollable = true
    ) {
        Text(
            text = routineName
                ?.let { stringResource(R.string.routine_sync_description, it) }
                ?: stringResource(R.string.routine_sync_description_generic),
            style = MaterialTheme.typography.bodyMedium,
            color = island.textSecondary
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TileShape)
                .background(island.sunkenSoft)
                .padding(vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            changes.forEach { change -> ChangeRow(change) }
        }

        Text(
            text = stringResource(R.string.routine_sync_targets_note),
            style = MaterialTheme.typography.bodySmall,
            color = island.textSecondary
        )

        IslandButton(
            text = stringResource(R.string.routine_sync_apply),
            onClick = onUpdate,
            modifier = Modifier.fillMaxWidth()
        )
        IslandSecondaryButton(
            text = stringResource(R.string.routine_sync_keep),
            onClick = onKeep,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChangeRow(change: RoutineChange) {
    val island = EinaTheme.island
    val icon = when (change) {
        is RoutineChange.Added -> Icons.Outlined.Add
        is RoutineChange.Removed -> Icons.Outlined.Delete
        is RoutineChange.SetsChanged -> Icons.Outlined.Repeat
        is RoutineChange.RestChanged -> Icons.Outlined.Timer
        RoutineChange.Reordered -> Icons.Outlined.SwapVert
    }
    val text = when (change) {
        is RoutineChange.Added -> stringResource(R.string.routine_sync_added, change.name.localized())
        is RoutineChange.Removed -> stringResource(R.string.routine_sync_removed, change.name.localized())
        is RoutineChange.SetsChanged ->
            stringResource(R.string.routine_sync_sets, change.name.localized(), change.from, change.to)
        is RoutineChange.RestChanged -> stringResource(
            R.string.routine_sync_rest,
            change.name.localized(),
            formatClock(change.fromSeconds),
            formatClock(change.toSeconds)
        )
        RoutineChange.Reordered -> stringResource(R.string.routine_sync_reordered)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = island.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
