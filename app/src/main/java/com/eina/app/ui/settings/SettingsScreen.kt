package com.eina.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    var confirmClear by remember { mutableStateOf(false) }
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
                description = "Micro-vibrazione a ogni tocco: bottoni, chip, menu e check delle serie.",
                checked = haptics,
                onCheckedChange = viewModel::setHaptics
            )
        }

        SectionHeader(title = "Dati")

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text("Cancella lo storico", style = MaterialTheme.typography.titleSmall)
            Text(
                "Elimina tutti gli allenamenti registrati, serie comprese. Routine, esercizi e peso corporeo restano.",
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
            IslandSecondaryButton(
                text = "Cancella storico allenamenti",
                icon = Icons.Outlined.DeleteSweep,
                onClick = { confirmClear = true },
                contentColor = DestructiveRed,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirmClear) {
        // Operazione irreversibile: si conferma prima di toccare il database.
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = IslandShape,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Cancellare tutto lo storico?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Tutti gli allenamenti registrati verranno eliminati. L'operazione non e' annullabile.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearHistory() }) {
                    Text("Cancella", color = DestructiveRed)
                }
            },
            dismissButton = {
                // Neutro, non accentato: con la palette arancio "Annulla" primario si confondeva
                // col rosso di "Cancella" e le due azioni sembravano la stessa cosa.
                TextButton(onClick = { confirmClear = false }) {
                    Text("Annulla", color = island.textSecondary)
                }
            }
        )
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
    val hapticTap = LocalHapticTap.current
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
            // Il tap sullo switch dell'aptica vibra anche quando lo si sta spegnendo: e' l'ultimo
            // feedback prima che il canale si chiuda, e conferma che il comando e' passato.
            onCheckedChange = { hapticTap(); onCheckedChange(it) },
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
