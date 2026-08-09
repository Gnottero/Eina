package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Serie pianificata di un esercizio in una routine.
 *
 * Fino alla Fase 25 la scheda diceva solo "3 serie da 10": un numero e basta, uguale per tutte.
 * Cosi' non si poteva scrivere un riscaldamento seguito da due serie a cedimento, che e' come le
 * schede sono fatte davvero. Ogni serie e' una riga, col suo tipo, come nell'allenamento.
 */
@Entity(
    tableName = "routine_sets",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineExerciseId")]
)
data class RoutineSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineExerciseId: Long,
    val setIndex: Int,
    /** Per gli esercizi a tempo sono secondi, come `actualReps` in sessione. */
    val targetReps: Int? = null,
    val targetWeight: Double? = null,
    val setType: SetType = SetType.NORMAL
)
