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
    val weeklyGoalDays: StateFlow<Int> = settings.weeklyGoalDays

    /**
     * Whether Health Connect is installed; without it the section has nothing to offer. The value
     * is cached rather than queried on every read: behind it is a PackageManager lookup and the
     * screen reads it during composition. It is refreshed on return from the permission sheet,
     * which also covers installing Health Connect from there.
     */
    private val _healthAvailable = MutableStateFlow(healthSync.isAvailable)
    val healthAvailable: StateFlow<Boolean> = _healthAvailable.asStateFlow()

    /** Health permissions to request; the screen passes them to the Health Connect contract. */
    val healthPermissions: Set<String> get() = healthSync.permissions

    private val _healthGranted = MutableStateFlow(false)
    val healthGranted: StateFlow<Boolean> = _healthGranted.asStateFlow()

    init {
        refreshHealthPermissions()
    }

    /** Ricontrolla il permesso: si torna qui dopo la richiesta e dopo un giro in Health Connect. */
    fun refreshHealthPermissions() {
        viewModelScope.launch {
            _healthAvailable.value = healthSync.isAvailable
            _healthGranted.value = runCatching { healthSync.hasPermissions() }.getOrDefault(false)
        }
    }

    fun setLanguage(language: AppLanguage) = settings.setLanguage(language)
    fun setHaptics(enabled: Boolean) = settings.setHapticsEnabled(enabled)
    fun setWeeklyGoalDays(days: Int) = settings.setWeeklyGoalDays(days)
    fun setTimerSound(enabled: Boolean) = settings.setTimerSoundEnabled(enabled)
    fun setHealthSync(enabled: Boolean) = settings.setHealthSyncEnabled(enabled)
    fun setTimerVibration(enabled: Boolean) = settings.setTimerVibrationEnabled(enabled)

    /** Svuota lo storico allenamenti. Routine, esercizi e peso corporeo restano. */
    fun clearHistory() {
        viewModelScope.launch { workoutRepository.deleteAllSessions() }
    }
}
