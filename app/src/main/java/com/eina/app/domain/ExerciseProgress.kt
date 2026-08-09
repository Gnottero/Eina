package com.eina.app.domain

import com.eina.app.data.db.CompletedSetRow
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking

/**
 * Un punto della progressione: la serie migliore di un allenamento, con la data di quel giorno.
 * `weight` sono kg (o km sugli esercizi a distanza) e `reps` sono ripetizioni, secondi o minuti
 * a seconda del [WeightType] — le stesse due colonne della tabella serie.
 */
data class ProgressPoint(
    val date: Long,
    val weight: Double?,
    val reps: Int?
)

/**
 * Progressione di un esercizio nel tempo, dal piu' vecchio al piu' recente: un punto per
 * allenamento, non uno per serie.
 *
 * DECISIONE: il punto e' la serie migliore della sessione, non la media. La media scende
 * appena si aggiunge una serie leggera in coda, e leggere un calo dove invece si e' lavorato
 * di piu' e' esattamente il contrario di quel che serve a un grafico di progressione.
 * "Migliore" e' il carico piu' alto dove un carico c'e' — sugli assistiti si legge come per i
 * PR, il numero piu' alto — e le ripetizioni piu' alte dove il carico non si digita.
 */
fun exerciseProgress(rows: List<CompletedSetRow>): List<ProgressPoint> =
    rows.filter { it.setType.countsAsWorking }
        .groupBy { it.sessionId }
        .mapNotNull { (_, sessionRows) ->
            val best = bestSet(sessionRows) ?: return@mapNotNull null
            ProgressPoint(
                date = sessionRows.first().sessionStart,
                weight = best.weight,
                reps = best.actualReps
            )
        }
        .sortedBy { it.date }

private fun bestSet(rows: List<CompletedSetRow>): CompletedSetRow? = when (rows.first().weightType) {
    WeightType.FREE_WEIGHT,
    WeightType.MACHINE_STACK,
    WeightType.ASSISTED,
    WeightType.BODYWEIGHT_PLUS_LOAD ->
        // A parita' di carico vince la serie con piu' ripetizioni: e' andata meglio.
        rows.maxWithOrNull(compareBy({ it.weight ?: 0.0 }, { it.actualReps ?: 0 }))

    // Sulla distanza a parita' di chilometri vince chi ci ha messo meno, non chi ci ha messo
    // piu' minuti: si confronta la velocita', come per i PR.
    WeightType.DISTANCE_BASED ->
        rows.maxWithOrNull(compareBy({ it.weight ?: 0.0 }, { it.speedKmPerHour() }))

    WeightType.BODYWEIGHT, WeightType.TIME_BASED ->
        rows.maxByOrNull { it.actualReps ?: 0 }
}

/** Km/h della serie a distanza: `weight` sono chilometri e `actualReps` minuti. */
private fun CompletedSetRow.speedKmPerHour(): Double {
    val minutes = actualReps ?: 0
    if (minutes <= 0) return 0.0
    return (weight ?: 0.0) / (minutes / 60.0)
}
