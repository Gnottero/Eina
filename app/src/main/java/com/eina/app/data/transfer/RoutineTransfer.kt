package com.eina.app.data.transfer

import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.WeightType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Formato di scambio delle routine: un file JSON che si manda per messaggio o email, pensato
 * per il personal trainer che consegna una scheda.
 *
 * Gli esercizi viaggiano col loro nome inglese, che e' la chiave della libreria: chi importa
 * riusa l'esercizio che ha gia'. Il file porta comunque tipo di carico, muscoli e attrezzatura,
 * cosi' un esercizio inventato dal trainer si ricrea come esercizio custom invece di far
 * fallire l'import.
 *
 * Nessun id nel file: gli id sono locali al database di chi esporta e non significano niente
 * altrove.
 */
object RoutineTransfer {

    const val FORMAT = "eina.routine"
    const val VERSION = 1
    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "json"

    data class ExercisePayload(
        val name: String,
        val weightType: WeightType,
        val muscleGroupsPrimary: List<String>,
        val muscleGroupsSecondary: List<String>,
        val equipment: String?,
        val description: String,
        val targetSets: Int,
        val targetReps: Int,
        val targetWeight: Double?,
        val restSeconds: Int,
        val notes: String?
    )

    data class RoutinePayload(
        val name: String,
        val notes: String?,
        val linkedPlaylistUri: String?,
        val linkedPlaylistType: PlaylistType?,
        val exercises: List<ExercisePayload>
    )

    fun encode(
        routine: RoutineEntity,
        routineExercises: List<RoutineExerciseEntity>,
        exercisesById: Map<Long, ExerciseEntity>
    ): String {
        val exercises = JSONArray()
        routineExercises.sortedBy { it.order }.forEach { routineExercise ->
            val exercise = exercisesById[routineExercise.exerciseId] ?: return@forEach
            exercises.put(
                JSONObject().apply {
                    put("name", exercise.name)
                    put("weightType", exercise.weightType.name)
                    put("muscleGroupsPrimary", JSONArray(exercise.muscleGroupsPrimary))
                    put("muscleGroupsSecondary", JSONArray(exercise.muscleGroupsSecondary))
                    put("equipment", exercise.equipment ?: JSONObject.NULL)
                    put("description", exercise.description)
                    put("targetSets", routineExercise.targetSets)
                    put("targetReps", routineExercise.targetReps)
                    put("targetWeight", routineExercise.targetWeight ?: JSONObject.NULL)
                    put("restSeconds", routineExercise.restSeconds)
                    put("notes", routineExercise.notes ?: JSONObject.NULL)
                }
            )
        }

        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put(
                "routine",
                JSONObject().apply {
                    put("name", routine.name)
                    put("notes", routine.notes ?: JSONObject.NULL)
                    put("linkedPlaylistUri", routine.linkedPlaylistUri ?: JSONObject.NULL)
                    put("linkedPlaylistType", routine.linkedPlaylistType?.name ?: JSONObject.NULL)
                }
            )
            put("exercises", exercises)
        }.toString(2)
    }

    /**
     * Legge un file di scambio. Ritorna null se non e' una routine Eina o se e' scritta da una
     * versione futura del formato: meglio dirlo che importare una scheda a meta'.
     */
    fun decode(json: String): RoutinePayload? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (root.optString("format") != FORMAT) return null
        if (root.optInt("version", 0) !in 1..VERSION) return null
        val routine = root.optJSONObject("routine") ?: return null
        val exercises = root.optJSONArray("exercises") ?: JSONArray()

        val payloads = (0 until exercises.length()).mapNotNull { index ->
            val item = exercises.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            ExercisePayload(
                name = name,
                weightType = runCatching { WeightType.valueOf(item.optString("weightType")) }
                    .getOrDefault(WeightType.FREE_WEIGHT),
                muscleGroupsPrimary = item.optJSONArray("muscleGroupsPrimary").toStringList(),
                muscleGroupsSecondary = item.optJSONArray("muscleGroupsSecondary").toStringList(),
                equipment = item.optNullableString("equipment"),
                description = item.optString("description"),
                targetSets = item.optInt("targetSets", 3).coerceIn(1, 20),
                targetReps = item.optInt("targetReps", 10).coerceIn(1, 999),
                targetWeight = item.optNullableDouble("targetWeight"),
                restSeconds = item.optInt("restSeconds", 90).coerceIn(0, 3600),
                notes = item.optNullableString("notes")
            )
        }

        return RoutinePayload(
            name = routine.optString("name"),
            notes = routine.optNullableString("notes"),
            linkedPlaylistUri = routine.optNullableString("linkedPlaylistUri"),
            linkedPlaylistType = routine.optNullableString("linkedPlaylistType")
                ?.let { type -> runCatching { PlaylistType.valueOf(type) }.getOrNull() },
            exercises = payloads
        )
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) getDouble(name) else null
}
