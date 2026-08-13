package com.eina.app.data.seed

import android.content.Context
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.WeightType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Aligns the exercise library with the curated catalog in app/src/main/assets/seed/exercises.json.
 *
 * More than a "seed if empty": the catalog evolves between releases, so existing installs must be
 * realigned. Synchronisation happens by name:
 *   - new names are inserted,
 *   - existing ones are updated, translations included,
 *   - names dropped from the catalog are deleted only if no routine or workout uses them.
 * Custom exercises are never touched.
 *
 * The work runs only when CATALOG_VERSION changes; otherwise startup does not even read the JSON.
 */
class ExerciseSeeder(
    private val context: Context,
    private val exerciseDao: ExerciseDao
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun seedIfEmpty() {
        val alreadySynced = prefs.getInt(KEY_CATALOG_VERSION, 0) >= CATALOG_VERSION
        if (alreadySynced && exerciseDao.getCount() > 0) return

        val json = context.assets.open(SEED_ASSET_PATH).bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val catalog = (0 until array.length()).map { i -> array.getJSONObject(i).toExerciseEntity() }

        val existing = exerciseDao.getLibraryExercises().associateBy { it.name }

        val (toUpdate, toInsert) = catalog.partition { it.name in existing }

        exerciseDao.insertAll(toInsert)
        // copy(id = ...) instead of insert-replace: replacing the row would change the id and
        // detach the routines and history pointing at that exercise.
        exerciseDao.updateAll(toUpdate.map { it.copy(id = existing.getValue(it.name).id) })

        val obsolete = existing.keys - catalog.map { it.name }.toSet()
        if (obsolete.isNotEmpty()) {
            // SQLite caps the parameters of a query, so deletion happens in chunks.
            obsolete.chunked(400).forEach { exerciseDao.deleteUnusedLibraryExercises(it) }
        }

        prefs.edit().putInt(KEY_CATALOG_VERSION, CATALOG_VERSION).apply()
    }

    private fun JSONObject.toExerciseEntity(): ExerciseEntity = ExerciseEntity(
        name = getString("name"),
        nameIt = optNullableString("nameIt"),
        nameFr = optNullableString("nameFr"),
        description = getString("description"),
        descriptionIt = optNullableString("descriptionIt"),
        descriptionFr = optNullableString("descriptionFr"),
        // Deliberately empty: for library exercises the wording comes from weightType via
        // strings.xml. See ExerciseEntity.loggingInstructions.
        loggingInstructions = "",
        weightType = WeightType.valueOf(getString("weightType")),
        // Absent for exercises with an external load, where bodyweight does not enter the volume
        // and the field is ignored.
        bodyweightFactor = optDouble("bodyweightFactor", 1.0),
        muscleGroupsPrimary = getJSONArray("muscleGroupsPrimary").toStringList(),
        muscleGroupsSecondary = getJSONArray("muscleGroupsSecondary").toStringList(),
        equipment = optNullableString("equipment"),
        mediaUri = bundledMediaUri(optNullableString("mediaUri")),
        isCustom = false,
        source = optNullableString("source")
    )

    /**
     * Bundled image of the exercise, or null when the assets hold nothing for that movement.
     * The catalog references the original free-exercise-db JPGs ("Squat/0.jpg"); the folder of the
     * same name in assets may contain:
     *   - `anim.webp`: the anatomical animation with the worked muscles coloured
     *     (tools/fetch_exercise_gifs.py), always preferred;
     *   - `0.webp` / `1.webp`: the two photographic frames (tools/fetch_exercise_media.py), left
     *     only where no suitable animation exists.
     * The UI derives the second frame by convention (ui/components/ExerciseAnimation.kt).
     */
    private fun bundledMediaUri(catalogPath: String?): String? {
        val path = catalogPath?.takeIf { it.isNotBlank() } ?: return null
        val folder = path.substringBefore('/')
        val animPath = "media/$folder/anim.webp"
        if (animPath in bundledMedia) return "file:///android_asset/$animPath"
        val assetPath = "media/" + path.substringBeforeLast('.') + ".webp"
        if (assetPath !in bundledMedia) return null
        return "file:///android_asset/$assetPath"
    }

    /** WebP files present in assets/media: listed once, then checked in memory. */
    private val bundledMedia: Set<String> by lazy {
        val folders = context.assets.list("media")?.toList().orEmpty()
        folders.flatMap { folder ->
            context.assets.list("media/$folder")?.map { "media/$folder/$it" }.orEmpty()
        }.toSet()
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    companion object {
        private const val SEED_ASSET_PATH = "seed/exercises.json"
        private const val PREFS_NAME = "eina_seed"
        private const val KEY_CATALOG_VERSION = "catalog_version"

        /** Bump on every regeneration of exercises.json that changes its content. */
        private const val CATALOG_VERSION = 8
    }
}
