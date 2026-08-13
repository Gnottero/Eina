package com.eina.app.ui.library

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.eina.app.R
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.ExerciseName
import com.eina.app.data.db.WeightType
import com.eina.app.data.db.exerciseName
import java.util.Locale

/**
 * Exercise labels in the active language.
 *
 * The dataset arrives in English and stays English in the database: muscles and equipment are keys
 * ("chest", "barbell"), not display labels. Translation happens here, at draw time, so switching
 * language does not require rewriting the library. An unknown key — a custom exercise with
 * hand-written equipment, say — is shown as is rather than dropped.
 */

fun WeightType.labelRes(): Int = when (this) {
    WeightType.FREE_WEIGHT -> R.string.weight_type_free_weight
    WeightType.BODYWEIGHT -> R.string.weight_type_bodyweight
    WeightType.BODYWEIGHT_PLUS_LOAD -> R.string.weight_type_bodyweight_plus_load
    WeightType.ASSISTED -> R.string.weight_type_assisted
    WeightType.MACHINE_STACK -> R.string.weight_type_machine_stack
    WeightType.TIME_BASED -> R.string.weight_type_time_based
    WeightType.DISTANCE_BASED -> R.string.weight_type_distance_based
}

@Composable
fun WeightType.label(): String = LocalContext.current.getString(labelRes())

/** What to type in the weight field; depends only on the weight type. */
fun WeightType.loggingInstructionsRes(): Int = when (this) {
    WeightType.FREE_WEIGHT -> R.string.logging_free_weight
    WeightType.BODYWEIGHT -> R.string.logging_bodyweight
    WeightType.BODYWEIGHT_PLUS_LOAD -> R.string.logging_bodyweight_plus_load
    WeightType.ASSISTED -> R.string.logging_assisted
    WeightType.MACHINE_STACK -> R.string.logging_machine_stack
    WeightType.TIME_BASED -> R.string.logging_time_based
    WeightType.DISTANCE_BASED -> R.string.logging_distance_based
}

private val muscleLabels: Map<String, Int> = mapOf(
    "chest" to R.string.muscle_chest,
    "lats" to R.string.muscle_lats,
    "middle back" to R.string.muscle_middle_back,
    "lower back" to R.string.muscle_lower_back,
    "traps" to R.string.muscle_traps,
    "neck" to R.string.muscle_neck,
    "shoulders" to R.string.muscle_shoulders,
    "biceps" to R.string.muscle_biceps,
    "triceps" to R.string.muscle_triceps,
    "forearms" to R.string.muscle_forearms,
    "abdominals" to R.string.muscle_abdominals,
    "quadriceps" to R.string.muscle_quadriceps,
    "hamstrings" to R.string.muscle_hamstrings,
    "calves" to R.string.muscle_calves,
    "glutes" to R.string.muscle_glutes,
    "adductors" to R.string.muscle_adductors,
    "abductors" to R.string.muscle_abductors,
    // Not a dataset muscle: the key of an exercise working none of the listed groups.
    // See MuscleGroupCategory.OTHER.
    "other" to R.string.muscle_category_other
)

private val equipmentLabels: Map<String, Int> = mapOf(
    "barbell" to R.string.equipment_barbell,
    "dumbbell" to R.string.equipment_dumbbell,
    "cable" to R.string.equipment_cable,
    "machine" to R.string.equipment_machine,
    "body only" to R.string.equipment_body_only,
    "kettlebells" to R.string.equipment_kettlebells,
    "bands" to R.string.equipment_bands,
    "medicine ball" to R.string.equipment_medicine_ball,
    "exercise ball" to R.string.equipment_exercise_ball,
    "foam roll" to R.string.equipment_foam_roll,
    "e-z curl bar" to R.string.equipment_ez_curl_bar,
    "other" to R.string.equipment_other
)

fun Context.muscleLabel(muscle: String): String =
    muscleLabels[muscle.lowercase()]?.let { getString(it) } ?: muscle

@Composable
fun muscleLabel(muscle: String): String = LocalContext.current.muscleLabel(muscle)

fun Context.equipmentLabel(equipment: String): String =
    equipmentLabels[equipment.lowercase()]?.let { getString(it) } ?: equipment

@Composable
fun equipmentLabel(equipment: String): String = LocalContext.current.equipmentLabel(equipment)

/** Locale the UI is drawing with; follows the language chosen in Settings. */
@Composable
fun currentLocale(): Locale = LocalContext.current.resources.configuration.locales[0]

/** Exercise name in the active language, falling back to English. */
@Composable
fun ExerciseName.localized(): String = localized(currentLocale())

@Composable
fun ExerciseEntity.localizedName(): String = exerciseName().localized(currentLocale())

/**
 * Description in the active language, with English as a safety net: the catalog is translated, but
 * a custom or half-translated exercise must not end up blank.
 */
fun ExerciseEntity.localizedDescription(locale: Locale): String = when (locale.language) {
    "it" -> descriptionIt
    "fr" -> descriptionFr
    else -> null
}?.takeIf { it.isNotBlank() } ?: description

@Composable
fun ExerciseEntity.localizedDescription(): String = localizedDescription(currentLocale())

/**
 * How to log the exercise: a hand-written note on a custom exercise wins, everything else uses the
 * standard wording of its weight type.
 */
@Composable
fun ExerciseEntity.localizedLoggingInstructions(): String =
    loggingInstructions.takeIf { it.isNotBlank() }
        ?: LocalContext.current.getString(weightType.loggingInstructionsRes())
