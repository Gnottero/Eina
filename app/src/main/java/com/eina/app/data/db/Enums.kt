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

enum class PlaylistType { SPOTIFY, YOUTUBE_MUSIC }
