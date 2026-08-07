package com.eina.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SessionSummaryCard
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Storico",
                subtitle = if (sessions.isEmpty()) {
                    "Nessun allenamento"
                } else {
                    "${sessions.size} allenamenti registrati"
                },
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        if (sessions.isEmpty()) {
            IslandEmptyState(
                title = "Nessun allenamento registrato",
                description = "Le sessioni completate compaiono qui, dalla piu' recente.",
                icon = Icons.Outlined.History
            )
        } else {
            sessions.forEach { session ->
                SessionSummaryCard(
                    summary = session,
                    onClick = { onSessionClick(session.sessionId) }
                )
            }
        }
    }
}
