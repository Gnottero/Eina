package com.eina.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.eina.app.R

/** Groups the raw English muscle names of the dataset into the categories defined in CLAUDE.md. */
enum class MuscleGroupCategory(@StringRes val labelRes: Int, val color: Color) {
    CHEST_PUSH(R.string.muscle_category_chest_push, MuscleGroupColors.ChestPush),
    BACK_PULL(R.string.muscle_category_back_pull, MuscleGroupColors.BackPull),
    LEGS(R.string.muscle_category_legs, MuscleGroupColors.Legs),
    SHOULDERS(R.string.muscle_category_shoulders, MuscleGroupColors.Shoulders),
    ARMS(R.string.muscle_category_arms, MuscleGroupColors.Arms),
    CORE(R.string.muscle_category_core, MuscleGroupColors.Core),
    NECK(R.string.muscle_category_neck, MuscleGroupColors.Neck),
    // "Other" means a movement working none of the listed groups (mobility, balance, general
    // warm-up). It is also where a muscle unknown to the dataset falls.
    OTHER(R.string.muscle_category_other, MuscleGroupColors.Other)
}

@Composable
fun MuscleGroupCategory.label(): String = stringResource(labelRes)

private val muscleToCategory: Map<String, MuscleGroupCategory> = mapOf(
    "chest" to MuscleGroupCategory.CHEST_PUSH,
    "lats" to MuscleGroupCategory.BACK_PULL,
    "middle back" to MuscleGroupCategory.BACK_PULL,
    "lower back" to MuscleGroupCategory.BACK_PULL,
    "traps" to MuscleGroupCategory.BACK_PULL,
    "quadriceps" to MuscleGroupCategory.LEGS,
    "hamstrings" to MuscleGroupCategory.LEGS,
    "calves" to MuscleGroupCategory.LEGS,
    "glutes" to MuscleGroupCategory.LEGS,
    "abductors" to MuscleGroupCategory.LEGS,
    "adductors" to MuscleGroupCategory.LEGS,
    "shoulders" to MuscleGroupCategory.SHOULDERS,
    "biceps" to MuscleGroupCategory.ARMS,
    "triceps" to MuscleGroupCategory.ARMS,
    "forearms" to MuscleGroupCategory.ARMS,
    "abdominals" to MuscleGroupCategory.CORE,
    "neck" to MuscleGroupCategory.NECK,
    // Key written by the custom exercise form when "Other" is chosen.
    "other" to MuscleGroupCategory.OTHER
)

/** Category of a muscle; a name unknown to the dataset falls back to "Other". */
fun categoryFor(muscle: String): MuscleGroupCategory =
    muscleToCategory[muscle.lowercase()] ?: MuscleGroupCategory.OTHER

/** Primary category of an exercise: the one of its first primary muscle. */
fun primaryCategoryFor(muscleGroupsPrimary: List<String>): MuscleGroupCategory =
    muscleGroupsPrimary.firstOrNull()?.let { categoryFor(it) } ?: MuscleGroupCategory.OTHER

/** Canonical muscle key for a category, written when a category is picked in the custom form. */
fun canonicalMuscleKey(category: MuscleGroupCategory): String =
    muscleToCategory.entries.firstOrNull { it.value == category }?.key ?: "neck"
