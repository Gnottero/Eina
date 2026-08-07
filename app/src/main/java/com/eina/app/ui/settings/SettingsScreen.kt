package com.eina.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val sound by viewModel.timerSoundEnabled.collectAsState()
    val vibration by viewModel.timerVibrationEnabled.collectAsState()

    IslandScreen(
        header = {
            ScreenHeader(
                title = "Impostazioni",
                subtitle = "Feedback e comportamento dell'app",
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SectionHeader(title = "Timer di recupero")

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = "Suono a fine recupero",
                description = "Riproduce un breve avviso quando il timer arriva a zero.",
                checked = sound,
                onCheckedChange = viewModel::setTimerSound
            )
            SettingSwitch(
                title = "Vibrazione a fine recupero",
                description = "Doppia vibrazione all'esaurimento del timer.",
                checked = vibration,
                onCheckedChange = viewModel::setTimerVibration
            )
        }

        SectionHeader(title = "Feedback aptico")

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = "Vibrazione sui comandi",
                description = "Micro-vibrazione quando completi una serie.",
                checked = haptics,
                onCheckedChange = viewModel::setHaptics
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val island = EinaTheme.island
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = island.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = island.sunken,
                uncheckedBorderColor = island.outlineSubtle
            )
        )
    }
}
