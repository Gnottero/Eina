package com.eina.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferenze locali dell'app. DECISIONE: SharedPreferences invece di DataStore, per non
 * aggiungere una dipendenza a un solo pugno di flag booleani.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _timerSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_TIMER_SOUND, true))
    val timerSoundEnabled: StateFlow<Boolean> = _timerSoundEnabled.asStateFlow()

    private val _timerVibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_TIMER_VIBRATION, true))
    val timerVibrationEnabled: StateFlow<Boolean> = _timerVibrationEnabled.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) = update(KEY_HAPTICS, enabled, _hapticsEnabled)

    fun setTimerSoundEnabled(enabled: Boolean) = update(KEY_TIMER_SOUND, enabled, _timerSoundEnabled)

    fun setTimerVibrationEnabled(enabled: Boolean) = update(KEY_TIMER_VIBRATION, enabled, _timerVibrationEnabled)

    private fun update(key: String, value: Boolean, state: MutableStateFlow<Boolean>) {
        prefs.edit().putBoolean(key, value).apply()
        state.value = value
    }

    private companion object {
        const val PREFS_NAME = "eina_settings"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_TIMER_SOUND = "timer_sound_enabled"
        const val KEY_TIMER_VIBRATION = "timer_vibration_enabled"
    }
}
