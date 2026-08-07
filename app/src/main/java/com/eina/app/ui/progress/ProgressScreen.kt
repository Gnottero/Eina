package com.eina.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandEmptyState
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSegmentedRow
import com.eina.app.ui.components.MiniBarChart
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.components.StatTile
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import java.time.LocalDate

private val dayInitials = listOf("L", "M", "M", "G", "V", "S", "D")

@Composable
fun ProgressScreen() {
    val island = EinaTheme.island
    // TODO(Fase 6): sostituire lo stato vuoto con volume, PR, heatmap e peso corporeo reali.
    val today = LocalDate.now()
    val startOfWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
    var selectedDay by remember { mutableIntStateOf(today.dayOfWeek.value - 1) }

    IslandScreen(
        header = { ScreenHeader(title = "Progressi", subtitle = "Settimana corrente") },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        IslandSegmentedRow(
            items = dayInitials,
            secondaryLabels = (0..6).map { startOfWeek.plusDays(it.toLong()).dayOfMonth.toString() },
            selectedIndex = selectedDay,
            onSelect = { selectedDay = it }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Volume",
                value = "–",
                unit = "kg",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Serie",
                value = "–",
                modifier = Modifier.weight(1f)
            )
        }

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Volume per giorno", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nessun dato ancora",
                style = MaterialTheme.typography.bodyMedium,
                color = island.textSecondary
            )
            MiniBarChart(
                values = List(7) { 0f },
                labels = dayInitials,
                highlightIndex = selectedDay,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = "Record personali")

        IslandEmptyState(
            title = "Ancora nessun PR",
            description = "Completa le serie durante un allenamento: i nuovi record compaiono qui.",
            icon = Icons.Outlined.EmojiEvents
        )

        SectionHeader(title = "Corpo")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            StatTile(
                label = "Peso",
                value = "–",
                unit = "kg",
                icon = Icons.Outlined.MonitorWeight,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Streak",
                value = "–",
                unit = "giorni",
                icon = Icons.Outlined.Whatshot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
