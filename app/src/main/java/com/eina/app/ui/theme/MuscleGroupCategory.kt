package com.eina.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.eina.app.R

/** Raggruppa i muscoli grezzi del dataset (in inglese) nelle categorie definite in CLAUDE.md. */
enum class MuscleGroupCategory(@StringRes val labelRes: Int, val color: Color) {
    CHEST_PUSH(R.string.muscle_category_chest_push, MuscleGroupColors.ChestPush),
    BACK_PULL(R.string.muscle_category_back_pull, MuscleGroupColors.BackPull),
    LEGS(R.string.muscle_category_legs, MuscleGroupColors.Legs),
    SHOULDERS(R.string.muscle_category_shoulders, MuscleGroupColors.Shoulders),
    ARMS(R.string.muscle_category_arms, MuscleGroupColors.Arms),
    CORE(R.string.muscle_category_core, MuscleGroupColors.Core),
    OTHER(R.string.muscle_category_other, Color(0xFF9E9E9E))
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
    "neck" to MuscleGroupCategory.OTHER
)

fun categoryFor(muscle: String): MuscleGroupCategory =
    muscleToCategory[muscle.lowercase()] ?: MuscleGroupCategory.OTHER

/** Categoria "primaria" di un esercizio: quella del primo muscolo primario, per badge in lista. */
fun primaryCategoryFor(muscleGroupsPrimary: List<String>): MuscleGroupCategory =
    muscleGroupsPrimary.firstOrNull()?.let { categoryFor(it) } ?: MuscleGroupCategory.OTHER

/** Chiave muscolo canonica per categoria: usata quando l'utente sceglie una categoria nel form esercizio custom. */
fun canonicalMuscleKey(category: MuscleGroupCategory): String =
    muscleToCategory.entries.firstOrNull { it.value == category }?.key ?: "neck"
