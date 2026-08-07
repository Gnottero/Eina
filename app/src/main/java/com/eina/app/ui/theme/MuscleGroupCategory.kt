package com.eina.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Raggruppa i muscoli grezzi del dataset (in inglese) nelle categorie definite in CLAUDE.md. */
enum class MuscleGroupCategory(val label: String, val color: Color) {
    CHEST_PUSH("Petto/Push", MuscleGroupColors.ChestPush),
    BACK_PULL("Schiena/Pull", MuscleGroupColors.BackPull),
    LEGS("Gambe", MuscleGroupColors.Legs),
    SHOULDERS("Spalle", MuscleGroupColors.Shoulders),
    ARMS("Braccia", MuscleGroupColors.Arms),
    CORE("Core", MuscleGroupColors.Core),
    OTHER("Altro", Color(0xFF9E9E9E))
}

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
