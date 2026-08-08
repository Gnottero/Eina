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
        nameIt = optNullableString("nameIt"),
        nameFr = optNullableString("nameFr"),
        description = getString("description"),
        descriptionIt = optNullableString("descriptionIt"),
        descriptionFr = optNullableString("descriptionFr"),
        // Vuota di proposito: per la libreria la spiegazione la fa weightType, tradotta
        // in strings.xml. Vedi ExerciseEntity.loggingInstructions.
        loggingInstructions = "",
        weightType = WeightType.valueOf(getString("weightType")),
        // Assente per gli esercizi con un carico esterno: li' il peso corporeo non entra
        // nel volume e il campo non viene guardato.
        bodyweightFactor = optDouble("bodyweightFactor", 1.0),
        muscleGroupsPrimary = getJSONArray("muscleGroupsPrimary").toStringList(),
        muscleGroupsSecondary = getJSONArray("muscleGroupsSecondary").toStringList(),
        equipment = optNullableString("equipment"),
        mediaUri = bundledMediaUri(optNullableString("mediaUri")),
        isCustom = false,
        source = optNullableString("source")
    )

    /**
     * Immagine bundlata dell'esercizio, o null se per quel movimento non c'e' niente in assets.
     * Il catalogo cita i JPG originali di free-exercise-db ("Squat/0.jpg"); in assets, nella
     * cartella con lo stesso nome, ci puo' essere:
     *   - `anim.webp`: l'animazione anatomica coi muscoli lavorati colorati
     *     (tools/fetch_exercise_gifs.py), che si preferisce sempre;
     *   - `0.webp` / `1.webp`: i due fotogrammi fotografici (tools/fetch_exercise_media.py),
     *     rimasti solo dove non esiste un'animazione adatta.
     * Il secondo fotogramma lo ricava la UI per convenzione (ui/components/ExerciseAnimation.kt).
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

    /** Elenco dei WebP presenti in assets/media: una lettura sola, poi si controlla in memoria. */
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

        /** Da alzare a ogni rigenerazione di exercises.json che cambia i contenuti. */
        private const val CATALOG_VERSION = 6
    }
}
