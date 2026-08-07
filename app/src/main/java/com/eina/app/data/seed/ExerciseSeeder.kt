package com.eina.app.data.seed

import android.content.Context
import com.eina.app.data.db.ExerciseDao
import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.WeightType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Allinea la libreria esercizi al catalogo curato in
 * app/src/main/assets/seed/exercises.json.
 *
 * Non e' piu' un semplice "seed se vuoto": il catalogo e' passato dai 873 esercizi grezzi
 * di free-exercise-db a un sottoinsieme curato e tradotto, quindi chi aveva gia' l'app
 * installata va riallineato. La sincronizzazione avviene per nome:
 *   - i nomi nuovi si inseriscono,
 *   - quelli gia' presenti si aggiornano (descrizioni tradotte comprese),
 *   - quelli spariti dal catalogo si eliminano solo se non usati da routine o allenamenti.
 * Gli esercizi custom dell'utente non vengono mai toccati.
 *
 * Il lavoro si ripete solo quando cambia CATALOG_VERSION: a regime l'avvio non legge
 * nemmeno il JSON.
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
        // copy(id = ...) e non insert-replace: sostituire la riga cambierebbe l'id e
        // scollegherebbe routine e storico che puntano a quell'esercizio.
        exerciseDao.updateAll(toUpdate.map { it.copy(id = existing.getValue(it.name).id) })

        val obsolete = existing.keys - catalog.map { it.name }.toSet()
        if (obsolete.isNotEmpty()) {
            // SQLite ha un tetto ai parametri di una query: si cancella a blocchi.
            obsolete.chunked(400).forEach { exerciseDao.deleteUnusedLibraryExercises(it) }
        }

        prefs.edit().putInt(KEY_CATALOG_VERSION, CATALOG_VERSION).apply()
    }

    private fun JSONObject.toExerciseEntity(): ExerciseEntity = ExerciseEntity(
        name = getString("name"),
        description = getString("description"),
        descriptionIt = optNullableString("descriptionIt"),
        descriptionFr = optNullableString("descriptionFr"),
        // Vuota di proposito: per la libreria la spiegazione la fa weightType, tradotta
        // in strings.xml. Vedi ExerciseEntity.loggingInstructions.
        loggingInstructions = "",
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
        private const val PREFS_NAME = "eina_seed"
        private const val KEY_CATALOG_VERSION = "catalog_version"

        /** Da alzare a ogni rigenerazione di exercises.json che cambia i contenuti. */
        private const val CATALOG_VERSION = 2
    }
}
