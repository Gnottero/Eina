package com.eina.app.ui.settings

import androidx.lifecycle.ViewModel
import com.eina.app.data.prefs.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled
    val timerSoundEnabled: StateFlow<Boolean> = settings.timerSoundEnabled
    val timerVibrationEnabled: StateFlow<Boolean> = settings.timerVibrationEnabled

    fun setHaptics(enabled: Boolean) = settings.setHapticsEnabled(enabled)
    fun setTimerSound(enabled: Boolean) = settings.setTimerSoundEnabled(enabled)
    fun setTimerVibration(enabled: Boolean) = settings.setTimerVibrationEnabled(enabled)
}
