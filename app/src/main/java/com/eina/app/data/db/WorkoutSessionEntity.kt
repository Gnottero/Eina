package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long? = null,
    val startTime: Long,
    val endTime: Long? = null,
    // What the watch measured during the workout, read from Health Connect when it ends. All
    // nullable: without a connected watch, or without the permission, the session is unchanged.
    val avgHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val caloriesKcal: Double? = null,
    /**
     * Heart rate over the workout, for the summary chart: comma-separated "instant:bpm" pairs in a
     * single column.
     *
     * DECISIONE: no samples table. These are read-only data shown in one summary and never queried
     * by value, so a table and its migration would not pay for themselves.
     */
    val heartRateSamples: String? = null
)

/** Heart rate samples, decoded from the compact column. */
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
