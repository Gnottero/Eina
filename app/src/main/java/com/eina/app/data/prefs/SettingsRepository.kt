package com.eina.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local app preferences. DECISIONE: SharedPreferences instead of DataStore, to avoid a dependency
 * for a handful of boolean flags.
 */
class SettingsRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(AppLocale.stored(context))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _timerSoundEnabled = MutableStateFlow(prefs.getBoolean(KEY_TIMER_SOUND, true))
    val timerSoundEnabled: StateFlow<Boolean> = _timerSoundEnabled.asStateFlow()

    private val _timerVibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_TIMER_VIBRATION, true))
    val timerVibrationEnabled: StateFlow<Boolean> = _timerVibrationEnabled.asStateFlow()

    /**
     * Reading watch data (heart rate, calories) from Health Connect. On by default: without the
     * system permission nothing is read anyway, so this switch exists to turn the feature off while
     * keeping the permission.
     */
    private val _healthSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_HEALTH_SYNC, true))
    val healthSyncEnabled: StateFlow<Boolean> = _healthSyncEnabled.asStateFlow()

    /**
     * The language does not apply on its own, since resources are already resolved: the caller
     * recreates the Activity so attachBaseContext goes through AppLocale.wrap again.
     */
    fun setLanguage(language: AppLanguage) {
        AppLocale.store(context, language)
        _language.value = language
    }

    fun setHapticsEnabled(enabled: Boolean) = update(KEY_HAPTICS, enabled, _hapticsEnabled)

    fun setTimerSoundEnabled(enabled: Boolean) = update(KEY_TIMER_SOUND, enabled, _timerSoundEnabled)

    fun setTimerVibrationEnabled(enabled: Boolean) = update(KEY_TIMER_VIBRATION, enabled, _timerVibrationEnabled)

    fun setHealthSyncEnabled(enabled: Boolean) = update(KEY_HEALTH_SYNC, enabled, _healthSyncEnabled)

    private fun update(key: String, value: Boolean, state: MutableStateFlow<Boolean>) {
        prefs.edit().putBoolean(key, value).apply()
        state.value = value
    }

    private companion object {
        const val PREFS_NAME = "eina_settings"
        const val KEY_HAPTICS = "haptics_enabled"
        const val KEY_TIMER_SOUND = "timer_sound_enabled"
        const val KEY_TIMER_VIBRATION = "timer_vibration_enabled"
        const val KEY_HEALTH_SYNC = "health_sync_enabled"
    }
}
