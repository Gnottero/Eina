package com.eina.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.eina.app.R
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
                    onClick = { onSessionClick(session.sessionId) }
                )
            }
        }
    }
}
