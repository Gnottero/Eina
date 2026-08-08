package com.eina.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.eina.app.R
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SessionSummaryCard
import com.eina.app.ui.components.SheetActionRow
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    // Come per routine ed esercizi: le azioni di un allenamento stanno nel foglio del tocco lungo.
    var actionsFor by remember { mutableStateOf<Long?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<Long?>(null) }

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.history_title),
                subtitle = if (sessions.isEmpty()) {
                    stringResource(R.string.history_none)
                } else {
                    pluralStringResource(R.plurals.history_count, sessions.size, sessions.size)
                },
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (sessions.isEmpty()) {
            IslandEmptyState(
                title = stringResource(R.string.history_empty_title),
                description = stringResource(R.string.history_empty_description),
                icon = Icons.Outlined.History
            )
        } else {
            sessions.forEach { session ->
                SessionSummaryCard(
                    summary = session,
                    onClick = { onSessionClick(session.sessionId) },
                    onLongClick = { actionsFor = session.sessionId }
                )
            }
        }
    }

    actionsFor?.let { sessionId ->
        IslandBottomSheet(
            onDismiss = { actionsFor = null },
            title = stringResource(R.string.history_session_sheet_title)
        ) {
            SheetActionRow(
                icon = Icons.Outlined.Delete,
                label = stringResource(R.string.history_delete_session),
                description = stringResource(R.string.history_delete_session_description),
                destructive = true,
                onClick = { actionsFor = null; confirmDeleteFor = sessionId }
            )
        }
    }

    confirmDeleteFor?.let { sessionId ->
        // Le serie registrate spariscono con la sessione: si conferma prima di toccare il database.
        AlertDialog(
            onDismissRequest = { confirmDeleteFor = null },
            shape = IslandShape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.history_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = { Text(stringResource(R.string.history_delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDeleteFor = null; viewModel.deleteSession(sessionId) }) {
                    Text(stringResource(R.string.action_delete), color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFor = null }) {
                    Text(stringResource(R.string.action_cancel), color = EinaTheme.island.textSecondary)
                }
            }
        )
    }
}
