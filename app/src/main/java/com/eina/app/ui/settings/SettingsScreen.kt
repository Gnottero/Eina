package com.eina.app.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eina.app.BuildConfig
import com.eina.app.R
import com.eina.app.data.prefs.AppLanguage
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.IslandSecondaryButton
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.components.SectionHeader
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val island = EinaTheme.island
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val language by viewModel.language.collectAsState()
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val sound by viewModel.timerSoundEnabled.collectAsState()
    val vibration by viewModel.timerVibrationEnabled.collectAsState()
    val healthSync by viewModel.healthSyncEnabled.collectAsState()
    val healthGranted by viewModel.healthGranted.collectAsState()
    // Il permesso salute non passa dal contratto dei permessi runtime: Health Connect ha il suo,
    // che apre la sua schermata di consenso.
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthPermissions() }

    IslandScreen(
        header = {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        SectionHeader(title = stringResource(R.string.settings_section_language))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.settings_language_description),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
            // FlowRow e non LazyRow: le lingue vanno a capo e si vedono tutte insieme,
            // senza scorrimento laterale che ne nasconde una.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                AppLanguage.entries.forEach { entry ->
                    IslandChip(
                        text = stringResource(entry.labelRes),
                        selected = entry == language,
                        onClick = {
                            if (entry != language) {
                                viewModel.setLanguage(entry)
                                // Le risorse della schermata sono gia' risolte: senza ricreare
                                // l'Activity resterebbe tutto nella lingua precedente.
                                (context as? Activity)?.recreate()
                            }
                        }
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.settings_section_timer))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = stringResource(R.string.settings_sound_title),
                description = stringResource(R.string.settings_sound_description),
                checked = sound,
                onCheckedChange = viewModel::setTimerSound
            )
            SettingSwitch(
                title = stringResource(R.string.settings_vibration_title),
                description = stringResource(R.string.settings_vibration_description),
                checked = vibration,
                onCheckedChange = viewModel::setTimerVibration
            )
        }

        if (viewModel.healthAvailable) {
            SectionHeader(title = stringResource(R.string.settings_section_health))

            IslandCard(modifier = Modifier.fillMaxWidth()) {
                SettingSwitch(
                    title = stringResource(R.string.settings_health_title),
                    description = stringResource(R.string.settings_health_description),
                    checked = healthSync,
                    onCheckedChange = viewModel::setHealthSync
                )
                if (!healthGranted) {
                    Text(
                        stringResource(R.string.settings_health_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = island.textSecondary
                    )
                    IslandSecondaryButton(
                        text = stringResource(R.string.settings_health_permission_action),
                        icon = Icons.Outlined.MonitorHeart,
                        onClick = { healthPermissionLauncher.launch(viewModel.healthPermissions) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.settings_section_haptics))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            SettingSwitch(
                title = stringResource(R.string.settings_haptics_title),
                description = stringResource(R.string.settings_haptics_description),
                checked = haptics,
                onCheckedChange = viewModel::setHaptics
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_support))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_donate_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.settings_donate_description),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
            IslandSecondaryButton(
                text = stringResource(R.string.settings_donate_title),
                icon = Icons.Outlined.Coffee,
                onClick = { launchDonationPage(context) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_data))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_clear_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.settings_clear_description),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
            IslandSecondaryButton(
                text = stringResource(R.string.settings_clear_button),
                icon = Icons.Outlined.DeleteSweep,
                onClick = { confirmClear = true },
                contentColor = DestructiveRed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Allenamento di prova: la sola via per vedere il riepilogo con battiti e calorie senza
        // un orologio collegato, e qualcosa di cancellabile su cui provare la correzione.
        IslandCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.settings_sample_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.settings_sample_description),
                style = MaterialTheme.typography.bodySmall,
                color = island.textSecondary
            )
            IslandSecondaryButton(
                text = stringResource(R.string.settings_sample_button),
                icon = Icons.Outlined.Science,
                onClick = {
                    viewModel.addSampleSession { added ->
                        Toast.makeText(
                            context,
                            context.getString(
                                if (added) R.string.settings_sample_added else R.string.settings_sample_failed
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionHeader(title = stringResource(R.string.settings_section_info))

        IslandCard(modifier = Modifier.fillMaxWidth()) {
            InfoRow(stringResource(R.string.info_version), BuildConfig.VERSION_NAME)
            InfoRow(stringResource(R.string.info_privacy), stringResource(R.string.info_privacy_value))
            InfoRow(stringResource(R.string.info_library), stringResource(R.string.info_library_value))
            InfoRow(stringResource(R.string.info_icons), stringResource(R.string.info_icons_value))
            InfoRow(stringResource(R.string.info_font), stringResource(R.string.info_font_value))
        }
    }

    if (confirmClear) {
        // Operazione irreversibile: si conferma prima di toccare il database.
        // "Annulla" resta neutro, non accentato: con la palette arancio un "Annulla" primario si
        // confondeva col rosso di "Cancella" e le due azioni sembravano la stessa cosa.
        IslandAlertDialog(
            title = stringResource(R.string.settings_clear_confirm_title),
            text = stringResource(R.string.settings_clear_confirm_text),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmClear = false; viewModel.clearHistory() },
            dismissLabel = stringResource(R.string.action_cancel),
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val island = EinaTheme.island
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.bodySmall, color = island.textSecondary)
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
