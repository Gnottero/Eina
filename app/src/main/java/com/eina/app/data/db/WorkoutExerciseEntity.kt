package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val order: Int,
    /**
     * Recupero dell'esercizio in questa sessione. Sta qui e non solo sulle serie: il recupero si
     * cambia in palestra, e leggerlo dalla prima serie lo faceva tornare al valore della scheda
     * appena quella serie era gia' segnata (una serie svolta non si tocca piu').
     */
    val restSeconds: Int = 90,
    /** Nota dell'esercizio in questa sessione: parte da quella della routine ed e' modificabile. */
    val notes: String? = null,
    /** Superset di questa sessione: parte da quello della routine ed e' modificabile qui. Vedi
     *  [RoutineExerciseEntity.supersetGroup]. */
    val supersetGroup: Int? = null
)
