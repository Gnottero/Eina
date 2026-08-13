package com.eina.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // English dataset name: the key ExerciseSeeder matches exercises by across catalog versions,
    // and the fallback when a translation is missing.
    val name: String,
    val nameIt: String? = null,
    val nameFr: String? = null,
    // Original English text from the dataset; also the fallback for missing translations.
    val description: String,
    val descriptionIt: String? = null,
    val descriptionFr: String? = null,
    // Empty for library exercises: their wording depends only on weightType and lives in
    // strings.xml, so it follows the app language. Only custom exercises fill it in.
    val loggingInstructions: String,
    val weightType: WeightType,
    // Share of bodyweight the movement actually lifts, from 0 to 1: it tells pull-ups (1, the
    // whole body rises) from crunches (0, nothing is lifted against gravity, and counting it
    // would inflate the session volume). Only relevant for BODYWEIGHT / BODYWEIGHT_PLUS_LOAD /
    // ASSISTED; the other weight types ignore it.
    val bodyweightFactor: Double = 1.0,
    val muscleGroupsPrimary: List<String>,     // TypeConverter: JSON string
    val muscleGroupsSecondary: List<String>,   // TypeConverter: JSON string
    val equipment: String? = null,
    val mediaUri: String? = null,              // local or bundled animation
    val isCustom: Boolean = false,
    val source: String? = null
)
