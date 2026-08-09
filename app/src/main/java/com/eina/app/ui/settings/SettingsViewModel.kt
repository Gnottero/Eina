package com.eina.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eina.app.data.prefs.AppLanguage
import com.eina.app.data.prefs.SettingsRepository
import com.eina.app.data.health.WorkoutHealthSync
import com.eina.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val workoutRepository: WorkoutRepository,
    private val healthSync: WorkoutHealthSync
) : ViewModel() {

    val language: StateFlow<AppLanguage> = settings.language
    val hapticsEnabled: StateFlow<Boolean> = settings.hapticsEnabled
    val timerSoundEnabled: StateFlow<Boolean> = settings.timerSoundEnabled
    val timerVibrationEnabled: StateFlow<Boolean> = settings.timerVibrationEnabled
    val healthSyncEnabled: StateFlow<Boolean> = settings.healthSyncEnabled

    /** Health Connect installato su questo telefono: senza, la sezione non ha niente da offrire. */
    val healthAvailable: Boolean get() = healthSync.isAvailable

    /** Permessi salute da chiedere al sistema: li passa la schermata al contratto di Health Connect. */
    val healthPermissions: Set<String> get() = healthSync.permissions

    private val _healthGranted = MutableStateFlow(false)
    val healthGranted: StateFlow<Boolean> = _healthGranted.asStateFlow()

    init {
        refreshHealthPermissions()
    }

    /** Ricontrolla il permesso: si torna qui dopo la richiesta e dopo un giro in Health Connect. */
    fun refreshHealthPermissions() {
        viewModelScope.launch {
            _healthGranted.value = runCatching { healthSync.hasPermissions() }.getOrDefault(false)
        }
    }

    fun setLanguage(language: AppLanguage) = settings.setLanguage(language)
    fun setHaptics(enabled: Boolean) = settings.setHapticsEnabled(enabled)
    fun setTimerSound(enabled: Boolean) = settings.setTimerSoundEnabled(enabled)
    fun setHealthSync(enabled: Boolean) = settings.setHealthSyncEnabled(enabled)
    fun setTimerVibration(enabled: Boolean) = settings.setTimerVibrationEnabled(enabled)

    /** Svuota lo storico allenamenti. Routine, esercizi e peso corporeo restano. */
    fun clearHistory() {
        viewModelScope.launch { workoutRepository.deleteAllSessions() }
    }
}
