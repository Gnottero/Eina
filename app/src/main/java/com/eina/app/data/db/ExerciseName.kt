package com.eina.app.data.db

import java.util.Locale

/**
 * Name of an exercise in the three languages.
 *
 * It travels whole to the UI instead of being resolved in the repository: the language is picked at
 * draw time, so changing it redraws the names without re-running the queries. English is also the
 * key the seeder matches exercises by, so it is never null and is the natural fallback for a custom
 * or untranslated exercise.
 */
data class ExerciseName(
    val nameEn: String,
    val nameIt: String? = null,
    val nameFr: String? = null
) {
    fun localized(locale: Locale): String = when (locale.language) {
        "it" -> nameIt
        "fr" -> nameFr
        else -> null
    }?.takeIf { it.isNotBlank() } ?: nameEn
}

fun ExerciseEntity.exerciseName(): ExerciseName = ExerciseName(name, nameIt, nameFr)

/**
 * Search by name. It looks at every language and not only the active one, so an exercise learned
 * under its English name is still found with the app in another language.
 */
fun ExerciseEntity.matchesQuery(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        nameIt?.contains(query, ignoreCase = true) == true ||
        nameFr?.contains(query, ignoreCase = true) == true
