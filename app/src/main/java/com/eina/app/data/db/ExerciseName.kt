package com.eina.app.data.db

import java.util.Locale

/**
 * Nome di un esercizio nelle tre lingue.
 *
 * Viaggia intero fino alla UI invece di essere risolto nel repository: la lingua si sceglie
 * al disegno, cosi' cambiarla ridisegna i nomi senza rifare le query. L'inglese e' anche la
 * chiave con cui il seeder riconosce l'esercizio, quindi non e' mai nullo ed e' il fallback
 * naturale per un esercizio custom o non ancora tradotto.
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
 * Ricerca per nome. Guarda tutte le lingue e non solo quella attiva: chi ha imparato un
 * esercizio col nome inglese lo cerca cosi' anche con l'app in italiano.
 */
fun ExerciseEntity.matchesQuery(query: String): Boolean =
    name.contains(query, ignoreCase = true) ||
        nameIt?.contains(query, ignoreCase = true) == true ||
        nameFr?.contains(query, ignoreCase = true) == true
