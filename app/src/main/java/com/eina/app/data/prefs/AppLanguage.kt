package com.eina.app.data.prefs

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import com.eina.app.R
import java.util.Locale

/**
 * App language. SYSTEM follows the phone, the others override it.
 *
 * The language labels stay written in their own language ("Italiano", "Français"): whoever opens
 * the menu to escape a language they do not read must be able to recognise their own.
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
 * Applies the chosen language without AppCompat, by rewriting the Context Configuration before the
 * Activity loads its resources. DECISIONE: some thirty lines against a dependency
 * (androidx.appcompat) this all-Compose app does not otherwise use.
 *
 * Locale.setDefault is needed on top of the Configuration: java.time and String.format read from
 * there and not from the resources, so without it dates would stay in the system language.
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

    /** To be called from attachBaseContext, before any resource is resolved. */
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
        // SYSTEM needs the phone language, not the one the app already overrode:
        // Resources.getSystem() is the only source immune to the override.
        ?: Resources.getSystem().configuration.locales[0]
}
