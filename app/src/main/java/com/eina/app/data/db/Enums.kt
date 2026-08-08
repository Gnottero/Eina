package com.eina.app.data.db

enum class WeightType {
    FREE_WEIGHT, BODYWEIGHT, BODYWEIGHT_PLUS_LOAD, ASSISTED, MACHINE_STACK, TIME_BASED
}

/**
 * Se la serie ha un carico da digitare. A corpo libero il peso e' il proprio (arriva da
 * `bodyweightSnapshotKg`) e a tempo non esiste: in entrambi i casi il campo kg sparisce dalla
 * tabella invece di restare li' a farsi ignorare.
 */
val WeightType.usesWeight: Boolean
    get() = this != WeightType.BODYWEIGHT && this != WeightType.TIME_BASED

/** Per gli esercizi a tempo `actualReps` sono secondi: cambia l'etichetta, non il campo. */
val WeightType.usesDuration: Boolean
    get() = this == WeightType.TIME_BASED

/**
 * Natura della singola serie. Il riscaldamento resta fuori da volume, PR e recupero; cedimento
 * e drop set sono serie di lavoro a tutti gli effetti, il segno serve a rileggerle nello storico.
 */
enum class SetType { WARMUP, NORMAL, FAILURE, DROP }

/** Una serie di riscaldamento non e' lavoro: non fa volume, non fa PR, non avvia il recupero. */
val SetType.countsAsWorking: Boolean
    get() = this != SetType.WARMUP

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }
