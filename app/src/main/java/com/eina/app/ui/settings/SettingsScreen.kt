package com.eina.app.ui.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.eina.app.BuildConfig
import com.eina.app.R
import com.eina.app.ui.components.DestructiveRed
import com.eina.app.ui.components.IslandAlertDialog
import com.eina.app.ui.components.IslandBottomSheet
import com.eina.app.ui.components.IslandCard
import com.eina.app.ui.components.IslandChip
import com.eina.app.ui.components.IslandScreen
import com.eina.app.ui.components.RampBand
import com.eina.app.ui.components.ScreenHeader
import com.eina.app.ui.feedback.LocalHapticTap
import com.eina.app.ui.theme.EinaTheme
import com.eina.app.ui.theme.IslandShape
import com.eina.app.ui.theme.Spacing
import com.eina.app.ui.theme.squircle
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showGoalPicker by remember { mutableStateOf(false) }
    val language by viewModel.language.collectAsState()
    val goalDays by viewModel.weeklyGoalDays.collectAsState()
    val haptics by viewModel.hapticsEnabled.collectAsState()
    val sound by viewModel.timerSoundEnabled.collectAsState()
    val vibration by viewModel.timerVibrationEnabled.collectAsState()
    val healthSync by viewModel.healthSyncEnabled.collectAsState()
    val healthGranted by viewModel.healthGranted.collectAsState()
    val healthAvailable by viewModel.healthAvailable.collectAsState()
    // The health permission does not go through the runtime permission contract: Health Connect
    // has its own, which opens its consent screen.
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthPermissions() }

    IslandScreen(
        header = {
            ScreenHeader(
                eyebrow = stringResource(R.string.settings_subtitle),
                title = stringResource(R.string.settings_title),
                onBack = onBack
            )
        },
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Every setting in one island, the groups marked by a small label inside it. As one card
        // per group the screen was a ladder of six white blocks, and the eye had to decide each
        // time whether a new block meant a new subject or just a new row.
        IslandCard(
            modifier = Modifier.fillMaxWidth(),
            shape = IslandShape,
            contentPadding = PaddingValues(bottom = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            SettingsGroup(stringResource(R.string.settings_section_language))
            SettingRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.settings_language_value),
                hint = stringResource(R.string.settings_language_description),
                value = stringResource(language.labelRes),
                onClick = { showLanguagePicker = true }
            )
            SettingRow(
                icon = Icons.Outlined.Flag,
                label = stringResource(R.string.settings_goal_title),
                hint = stringResource(R.string.settings_goal_description),
                value = pluralStringResource(R.plurals.day_count, goalDays, goalDays),
                onClick = { showGoalPicker = true }
            )

            SettingsGroup(stringResource(R.string.settings_section_timer))
            SettingRow(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                label = stringResource(R.string.settings_sound_title),
                hint = stringResource(R.string.settings_sound_description),
                checked = sound,
                onCheckedChange = viewModel::setTimerSound
            )
            SettingRow(
                icon = Icons.Outlined.Vibration,
                label = stringResource(R.string.settings_vibration_title),
                hint = stringResource(R.string.settings_vibration_description),
                checked = vibration,
                onCheckedChange = viewModel::setTimerVibration
            )

            if (healthAvailable) {
                SettingsGroup(stringResource(R.string.settings_section_health))
                SettingRow(
                    icon = Icons.Outlined.MonitorHeart,
                    label = stringResource(R.string.settings_health_title),
                    hint = stringResource(R.string.settings_health_description),
                    checked = healthSync,
                    onCheckedChange = viewModel::setHealthSync
                )
                if (!healthGranted) {
                    SettingRow(
                        icon = Icons.Outlined.MonitorHeart,
                        label = stringResource(R.string.settings_health_permission_action),
                        hint = stringResource(R.string.settings_health_permission_hint),
                        onClick = { healthPermissionLauncher.launch(viewModel.healthPermissions) }
                    )
                }
            }

            SettingsGroup(stringResource(R.string.settings_section_haptics))
            SettingRow(
                icon = Icons.Outlined.NotificationsActive,
                label = stringResource(R.string.settings_haptics_title),
                hint = stringResource(R.string.settings_haptics_description),
                checked = haptics,
                onCheckedChange = viewModel::setHaptics
            )

            SettingsGroup(stringResource(R.string.settings_section_data))
            SettingRow(
                icon = Icons.Outlined.DeleteSweep,
                label = stringResource(R.string.settings_clear_title),
                hint = stringResource(R.string.settings_clear_description),
                // Only the icon is red, as in the action sheets: the row is not dangerous, the
                // confirmation behind it is.
                iconTint = DestructiveRed,
                onClick = { confirmClear = true }
            )

            SettingsGroup(stringResource(R.string.settings_section_info))
            InfoRow(stringResource(R.string.info_version), BuildConfig.VERSION_NAME)
            InfoRow(stringResource(R.string.info_privacy), stringResource(R.string.info_privacy_value))
            InfoRow(stringResource(R.string.info_library), stringResource(R.string.info_library_value))
            InfoRow(stringResource(R.string.info_icons), stringResource(R.string.info_icons_value))
            InfoRow(stringResource(R.string.info_font), stringResource(R.string.info_font_value))

            // The donation lives inside the island as its coloured foot, not as a seventh card: it
            // is an offer, not a setting, and it is the one thing on this screen worth colour.
            DonationBlock(onDonate = { launchDonationPage(context) })
        }
    }

    if (showLanguagePicker) {
        LanguagePickerSheet(
            current = language,
            onSelect = { entry ->
                showLanguagePicker = false
                if (entry != language) {
                    viewModel.setLanguage(entry)
                    // The screen resources are already resolved: without recreating the Activity
                    // everything would stay in the previous language.
                    (context as? Activity)?.recreate()
                }
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showGoalPicker) {
        WeeklyGoalSheet(
            current = goalDays,
            onSelect = { viewModel.setWeeklyGoalDays(it); showGoalPicker = false },
            onDismiss = { showGoalPicker = false }
        )
    }

    if (confirmClear) {
        // Irreversible: confirmed before touching the database. The dismiss action stays neutral,
        // since an accented one was too close to the red of the destructive button.
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

/** Group label inside the settings island: the only thing separating one subject from the next. */
@Composable
private fun SettingsGroup(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EinaTheme.island.textSecondary,
        modifier = Modifier.padding(
            start = Spacing.lg,
            end = Spacing.lg,
            top = Spacing.lg,
            bottom = Spacing.xs
        )
    )
}

/**
 * Settings row: accent icon, label, supporting line, and on the right either a value that opens
 * something or a switch. One shape for every setting, whether it is a choice or a toggle.
 */
@Composable
private fun SettingRow(
    icon: ImageVector?,
    label: String,
    hint: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val island = EinaTheme.island
    val hapticTap = LocalHapticTap.current
    // A switch row is tapped anywhere on the row, not only on the switch: the label is a much
    // larger target than the 46dp track.
    val rowClick = onClick ?: onCheckedChange?.let { change -> { change(!(checked ?: false)) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm)
            .clip(squircle(18.dp))
            .then(if (rowClick == null) Modifier else Modifier.clickable { hapticTap(); rowClick() })
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Info rows carry no icon: five identical (i) glyphs down a column said nothing, and the
        // spacer keeps their labels on the same line as the settings above them.
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = island.textSecondary,
                // The hint is one short phrase; anything longer belongs on the screen it opens.
                // Unbounded, a four-line description made the row taller than the group above it.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = island.textSecondary
            )
        }
        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                // The haptics switch vibrates even while being turned off: it is the last feedback
                // before the channel closes and confirms the tap went through.
                onCheckedChange = { hapticTap(); onCheckedChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = island.textSecondary.copy(alpha = 0.25f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    SettingRow(icon = null, label = label, hint = value)
}

/** Foot of the settings island: the ramp, what the app costs, and the way to give anyway. */
@Composable
private fun DonationBlock(onDonate: () -> Unit) {
    val hapticTap = LocalHapticTap.current
    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .clip(squircle(22.dp))
    ) {
        RampBand(
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.settings_donate_headline),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.settings_donate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(squircle(12.dp))
                    .background(Color.White)
                    .clickable { hapticTap(); onDonate() }
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Coffee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.settings_donate_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = Spacing.sm)
                )
            }
        }
    }
}

/** Weekly goal: seven chips, because the answer is always a single digit. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyGoalSheet(current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    IslandBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_goal_sheet_title)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            (1..7).forEach { days ->
                IslandChip(
                    text = pluralStringResource(R.plurals.day_count, days, days),
                    selected = days == current,
                    onClick = { onSelect(days) }
                )
            }
        }
    }
}
