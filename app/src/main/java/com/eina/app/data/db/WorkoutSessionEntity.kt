package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val startTime: Long,
    val endTime: Long? = null,
    // Fase 29 (DB v9, MIGRATION_8_9): quel che l'orologio ha misurato mentre ci si allenava,
    // letto da Health Connect a fine allenamento. Tutto nullable: senza orologio collegato, o
    // senza permesso, la sessione resta quella di sempre.
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val caloriesKcal: Double? = null,
    /**
     * Battiti nel corso dell'allenamento, per la spezzata del riepilogo: coppie "istante:bpm"
     * separate da virgola, in una colonna sola.
     *
     * DECISIONE: niente tabella dei campioni. Sono dati di sola lettura, si mostrano solo nel
     * riepilogo di quella sessione e nessuna query li interroga per valore — una tabella con
     * la sua migrazione non pagherebbe se stessa.
     */
    val heartRateSamples: String? = null
)

/** Campioni della frequenza cardiaca, letti dalla colonna compatta. */
fun WorkoutSessionEntity.heartRateSeries(): List<HeartRateSample> =
    heartRateSamples?.split(',')
        ?.mapNotNull { chunk ->
            val parts = chunk.split(':')
            val time = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val bpm = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            HeartRateSample(time, bpm)
        }
        .orEmpty()

fun List<HeartRateSample>.encodeHeartRateSamples(): String? =
    takeIf { it.isNotEmpty() }?.joinToString(",") { "${it.timeMillis}:${it.bpm}" }

data class HeartRateSample(val timeMillis: Long, val bpm: Int)
