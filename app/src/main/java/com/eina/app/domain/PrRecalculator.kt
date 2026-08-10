package com.eina.app.domain

import com.eina.app.data.db.SetEntryEntity
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.countsAsWorking

/**
 * Ricalcola il flag `isPR` di tutte le serie di un esercizio.
 *
 * `isPR` e' l'unico dato di dominio che non si ricava da una query: viene scritto sulla riga nel
 * momento in cui la serie si chiude, confrontandola con quel che c'era prima. Finche' il passato
 * era immutabile bastava; da quando un allenamento gia' registrato si puo' correggere non basta
 * piu' — abbassare il carico di una serie del mese scorso lascerebbe il suo record in piedi, e
 * alzarlo non ne farebbe nascere uno.
 *
 * Quindi si riparte da zero: le serie si scorrono in ordine di completamento e ognuna e' un
 * record se batte tutte quelle prima di lei, esattamente come [isNewPR] al momento del tocco. Il
 * riscaldamento non fa mai record e non entra nel confronto.
 *
 * `sets` va passato completo (tutte le serie completate di quell'esercizio, in qualunque
 * sessione); l'ordine non conta, ci pensa la funzione. Ritorna le sole righe il cui flag cambia,
 * cosi' chi chiama scrive solo quelle.
 */
fun recomputePrFlags(weightType: WeightType, sets: List<SetEntryEntity>): List<SetEntryEntity> {
    // A parita' di istante vince l'ordine della serie nella sua sessione: due serie chiuse nello
    // stesso millisecondo esistono davvero (una sessione di prova, un import) e senza secondo
    // criterio l'esito dipenderebbe dall'ordine con cui il database le restituisce.
    val ordered = sets.sortedWith(compareBy({ it.completedAt ?: 0L }, { it.setIndex }, { it.id }))
    val history = mutableListOf<SetEntryEntity>()
    val changed = mutableListOf<SetEntryEntity>()

    ordered.forEach { set ->
        val isPR = set.completedAt != null && isNewPR(weightType, set, history)
        if (isPR != set.isPR) changed += set.copy(isPR = isPR)
        if (set.completedAt != null && set.setType.countsAsWorking) history += set
    }
    return changed
}
