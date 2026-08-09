package com.eina.app.data.db

enum class WeightType {
    FREE_WEIGHT, BODYWEIGHT, BODYWEIGHT_PLUS_LOAD, ASSISTED, MACHINE_STACK, TIME_BASED,

    /**
     * Tapis roulant, cyclette, ellittica: non c'e' un carico da sollevare, si registrano
     * distanza e tempo. Fase 27.
     *
     * DECISIONE: nessuna colonna nuova sulle serie. Come `actualReps` porta gia' i secondi
     * degli esercizi a tempo, qui `weight` porta i chilometri e `actualReps` i minuti — la
     * tabella e' la stessa, cambiano etichette e lettura. I minuti e non i secondi perche'
     * un allenamento in tapis roulant si scrive "30", non "1800".
     */
    DISTANCE_BASED
}

/**
 * Se la serie ha un carico da digitare. A corpo libero il peso e' il proprio (arriva da
 * `bodyweightSnapshotKg`), a tempo non esiste e sulla distanza il campo decimale porta i
 * chilometri: in tutti questi casi il campo kg sparisce dalla tabella invece di restare li'
 * a farsi ignorare.
 */
val WeightType.usesWeight: Boolean
    get() = this != WeightType.BODYWEIGHT &&
        this != WeightType.TIME_BASED &&
        this != WeightType.DISTANCE_BASED

/** Per gli esercizi a tempo `actualReps` sono secondi: cambia l'etichetta, non il campo. */
val WeightType.usesDuration: Boolean
    get() = this == WeightType.TIME_BASED

/** Per gli esercizi a distanza `weight` sono chilometri e `actualReps` minuti. */
val WeightType.usesDistance: Boolean
    get() = this == WeightType.DISTANCE_BASED

/** Se la tabella mostra il campo decimale: kg per i carichi, km per la distanza. */
val WeightType.usesDecimalField: Boolean
    get() = usesWeight || usesDistance

/**
 * Natura della singola serie. Il riscaldamento resta fuori da volume, PR e recupero; cedimento
 * e drop set sono serie di lavoro a tutti gli effetti, il segno serve a rileggerle nello storico.
 */
enum class SetType { WARMUP, NORMAL, FAILURE, DROP }

/** Una serie di riscaldamento non e' lavoro: non fa volume, non fa PR, non avvia il recupero. */
val SetType.countsAsWorking: Boolean
    get() = this != SetType.WARMUP

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }
