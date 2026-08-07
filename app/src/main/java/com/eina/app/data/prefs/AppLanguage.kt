package com.eina.app.data.prefs

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.eina.app.R
import java.util.Locale

/**
 * Lingua dell'app. SYSTEM segue il telefono, le altre lo scavalcano.
 *
 * Le etichette delle lingue restano scritte nella lingua stessa ("Italiano", "Français"):
 * chi apre il menu per uscire da una lingua che non capisce deve poter riconoscere la propria.
 */
enum class AppLanguage(val tag: String?, @StringRes val labelRes: Int) {
    SYSTEM(null, R.string.language_system),
    ENGLISH("en", R.string.language_english),
    ITALIAN("it", R.string.language_italian),
    FRENCH("fr", R.string.language_french);

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * Applica la lingua scelta senza passare da AppCompat: si riscrive la Configuration del
 * Context prima che l'Activity carichi le risorse. DECISIONE: e' una trentina di righe
 * contro una dipendenza (androidx.appcompat) che l'app, tutta Compose, non usa per altro.
 *
 * Locale.setDefault serve oltre alla Configuration: java.time e String.format leggono da li',
 * non dalle risorse, e senza questo le date resterebbero nella lingua di sistema.
 */
object AppLocale {

    private const val PREFS_NAME = "eina_settings"
    const val KEY_LANGUAGE = "app_language"

    fun stored(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))
    }

    fun store(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.tag)
            .apply()
    }

    /** Da chiamare in attachBaseContext, prima che vengano risolte le risorse. */
    fun wrap(base: Context): Context {
        val locale = localeFor(stored(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    private fun localeFor(language: AppLanguage): Locale = language.tag
        ?.let { Locale.forLanguageTag(it) }
        // Per SYSTEM serve la lingua del telefono, non quella gia' scavalcata dall'app:
        // Resources.getSystem() e' l'unica che resta immune all'override.
        ?: Resources.getSystem().configuration.locales[0]
}
