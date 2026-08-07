package com.eina.app.data.seed

import android.content.Context
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.WeightType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Importa app/src/main/assets/seed/exercises.json (dataset free-exercise-db arricchito)
 * al primo avvio, se la tabella exercises è vuota.
 */
class ExerciseSeeder(
    private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    suspend fun seedIfEmpty() {
        if (exerciseDao.getCount() > 0) return

        val json = context.assets.open(SEED_ASSET_PATH).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val entities = (0 until array.length()).map { i -> array.getJSONObject(i).toExerciseEntity() }
        exerciseDao.insertAll(entities)
    }

    private fun JSONObject.toExerciseEntity(): ExerciseEntity = ExerciseEntity(
        name = getString("name"),
        description = getString("description"),
        loggingInstructions = getString("loggingInstructions"),
        weightType = WeightType.valueOf(getString("weightType")),
        muscleGroupsPrimary = getJSONArray("muscleGroupsPrimary").toStringList(),
        muscleGroupsSecondary = getJSONArray("muscleGroupsSecondary").toStringList(),
        equipment = optNullableString("equipment"),
        // TODO: immagini esercizio non bundlate (vedi CLAUDE.md, sez. "Decisione sulle immagini esercizio").
        // mediaUri nel dataset punta a path relativi mai copiati in assets: ignorato finché non si decide
        // la strategia di bundling (miniature ridotte vs Play Asset Delivery vs subset curato).
        mediaUri = null,
        isCustom = false,
        source = optNullableString("source")
    )

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    companion object {
        private const val SEED_ASSET_PATH = "seed/exercises.json"
    }
}
