package com.eina.app.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferenze locali dell'app. DECISIONE: SharedPreferences invece di DataStore, per non
 * aggiungere una dipendenza a un solo pugno di flag booleani.
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
     * Lettura dei dati dell'orologio (battiti, calorie) da Health Connect. Acceso di suo: senza
     * il permesso di sistema non legge comunque niente, quindi non serve un secondo cancello —
     * l'interruttore serve a spegnerlo tenendo il permesso.
     */
    private val _healthSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_HEALTH_SYNC, true))
    val healthSyncEnabled: StateFlow<Boolean> = _healthSyncEnabled.asStateFlow()

    /**
     * La lingua non si applica da sola: le risorse sono gia' state risolte. Chi chiama
     * ricrea l'Activity, cosi' attachBaseContext ripassa da AppLocale.wrap.
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
