package com.eina.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled
    val timerSoundEnabled: StateFlow<Boolean> = settings.timerSoundEnabled
    val timerVibrationEnabled: StateFlow<Boolean> = settings.timerVibrationEnabled

    fun setHaptics(enabled: Boolean) = settings.setHapticsEnabled(enabled)
    fun setTimerSound(enabled: Boolean) = settings.setTimerSoundEnabled(enabled)
    fun setTimerVibration(enabled: Boolean) = settings.setTimerVibrationEnabled(enabled)

    /** Svuota lo storico allenamenti. Routine, esercizi e peso corporeo restano. */
    fun clearHistory() {
        viewModelScope.launch { workoutRepository.deleteAllSessions() }
    }
}
