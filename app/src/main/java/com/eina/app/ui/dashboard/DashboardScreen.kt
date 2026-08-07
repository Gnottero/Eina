package com.eina.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.IslandButton
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)
private val weekDayLabels = listOf("L", "M", "M", "G", "V", "S", "D")

@Composable
fun DashboardScreen(
    onStartWorkoutClick: () -> Unit = {}
) {
    val island = EinaTheme.island
    // TODO(Fase 6): collegare i dati reali (sessioni, volume, streak) tramite DashboardViewModel.
    // Finche' la Fase 6 non e' fatta le isole mostrano stato vuoto, non numeri finti.
    val today = LocalDate.now()

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Oggi",
                subtitle = today.format(dateFormatter).replaceFirstChar { it.uppercase() }
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Questa settimana",
                value = "–",
                unit = "sessioni",
                icon = Icons.Outlined.CalendarMonth,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Volume",
                value = "–",
                unit = "kg",
                icon = Icons.Outlined.FitnessCenter,
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Volume settimanale", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nessun dato ancora",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = List(7) { 0f },
                labels = weekDayLabels,
                highlightIndex = today.dayOfWeek.value - 1,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = "Ultimo allenamento")

        IslandEmptyState(
            title = "Nessun allenamento registrato",
            description = "Avvia una sessione: qui comparira' il riepilogo dell'ultima, con serie, volume e PR.",
            icon = Icons.Outlined.History
        )

        IslandButton(
            text = "Inizia allenamento",
            icon = Icons.Outlined.PlayArrow,
            onClick = onStartWorkoutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
