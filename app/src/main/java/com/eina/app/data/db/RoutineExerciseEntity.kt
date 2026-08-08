package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"])
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val order: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null,
    val restSeconds: Int,
    /** Nota libera sull'esercizio nella routine: viene copiata nella sessione all'avvio. */
    val notes: String? = null,
    /**
     * Superset: esercizi con lo stesso numero si eseguono a giro, uno dopo l'altro, e il recupero
     * parte solo quando il giro e' finito. null = esercizio a se'. I membri di un gruppo stanno
     * sempre uno di seguito all'altro in `order`.
     */
    val supersetGroup: Int? = null
)
