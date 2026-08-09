package com.eina.app.data.transfer

import com.eina.app.data.db.ExerciseEntity
import com.eina.app.data.db.PlaylistType
import com.eina.app.data.db.RoutineEntity
import com.eina.app.data.db.RoutineExerciseEntity
import com.eina.app.data.db.RoutineSetEntity
import com.eina.app.data.db.SetType
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
    // v2: le serie sono un elenco ("sets") invece delle tre colonne target. Un file v1 si legge
    // ancora, le sue targetSets diventano altrettante serie normali.
    const val VERSION = 2
    const val MIME_TYPE = "application/json"
    const val FILE_EXTENSION = "json"

    data class ExercisePayload(
        val name: String,
        val weightType: WeightType,
        val muscleGroupsPrimary: List<String>,
        val muscleGroupsSecondary: List<String>,
        val equipment: String?,
        val description: String,
        val sets: List<SetPayload>,
        val restSeconds: Int,
        val notes: String?,
        /** Superset di appartenenza, come numero di gruppo. null = esercizio a se'. */
        val supersetGroup: Int?
    )

    data class SetPayload(
        val targetReps: Int?,
        val targetWeight: Double?,
        val setType: SetType
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
        setsByRoutineExercise: Map<Long, List<RoutineSetEntity>>,
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
                    put(
                        "sets",
                        JSONArray().apply {
                            setsByRoutineExercise[routineExercise.id].orEmpty()
                                .sortedBy { it.setIndex }
                                .forEach { set ->
                                    put(
                                        JSONObject().apply {
                                            put("targetReps", set.targetReps ?: JSONObject.NULL)
                                            put("targetWeight", set.targetWeight ?: JSONObject.NULL)
                                            put("setType", set.setType.name)
                                        }
                                    )
                                }
                        }
                    )
                    put("restSeconds", routineExercise.restSeconds)
                    put("notes", routineExercise.notes ?: JSONObject.NULL)
                    put("supersetGroup", routineExercise.supersetGroup ?: JSONObject.NULL)
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
                sets = item.readSets(),
                restSeconds = item.optInt("restSeconds", 90).coerceIn(0, 3600),
                notes = item.optNullableString("notes"),
                // Campo nato dopo il formato v1: un file piu' vecchio semplicemente non ha superset.
                supersetGroup = item.optNullableInt("supersetGroup")
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

    /**
     * Serie dell'esercizio. Dal formato v2 sono un elenco; un file v1 porta solo targetSets/
     * targetReps/targetWeight e diventa quel numero di serie normali tutte uguali.
     */
    private fun JSONObject.readSets(): List<SetPayload> {
        val array = optJSONArray("sets")
        if (array != null) {
            return (0 until array.length()).mapNotNull { index ->
                val set = array.optJSONObject(index) ?: return@mapNotNull null
                SetPayload(
                    targetReps = set.optNullableInt("targetReps")?.coerceIn(1, 999),
                    targetWeight = set.optNullableDouble("targetWeight"),
                    setType = runCatching { SetType.valueOf(set.optString("setType")) }
                        .getOrDefault(SetType.NORMAL)
                )
            }
        }
        val count = optInt("targetSets", 3).coerceIn(1, 20)
        val reps = optInt("targetReps", 10).coerceIn(1, 999)
        val weight = optNullableDouble("targetWeight")
        return List(count) { SetPayload(targetReps = reps, targetWeight = weight, setType = SetType.NORMAL) }
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf { it.isNotBlank() } else null

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) getDouble(name) else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) getInt(name) else null
}
